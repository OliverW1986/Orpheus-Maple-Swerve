// Original Source:
// https://github.com/Mechanical-Advantage/AdvantageKit/tree/main/example_projects/advanced_swerve_drive/src/main, Copyright 2021-2024 FRC 6328
// Modified by 5516 Iron Maple https://github.com/Shenzhen-Robotics-Alliance/

package frc.robot;

import com.pathplanner.lib.auto.NamedCommands;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PowerDistribution;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import frc.robot.autos.Auto;
import frc.robot.autos.ExampleAuto;
import frc.robot.autos.PathPlannerAuto;
import frc.robot.commands.drive.*;
import frc.robot.subsystems.MapleSubsystem;
import frc.robot.subsystems.drive.*;
import frc.robot.subsystems.drive.IO.GyroIOPigeon2;
import frc.robot.subsystems.drive.IO.GyroIOSim;
import frc.robot.subsystems.drive.IO.ModuleIOSim;
import frc.robot.subsystems.drive.IO.ModuleIOTalonFX;
import frc.robot.subsystems.led.LEDStatusLight;
import frc.robot.subsystems.vision.apriltags.AprilTagVision;
import frc.robot.subsystems.vision.apriltags.AprilTagVisionIOReal;
import frc.robot.subsystems.vision.apriltags.ApriltagVisionIOSim;
import frc.robot.tests.*;
import frc.robot.utils.CompetitionFieldUtils.CompetitionFieldVisualizer;
import frc.robot.utils.CompetitionFieldUtils.Simulations.CompetitionFieldSimulation;
import frc.robot.utils.CompetitionFieldUtils.Simulations.Crescendo2024FieldSimulation;
import frc.robot.utils.CompetitionFieldUtils.Simulations.OpponentRobotSimulation;
import frc.robot.utils.CompetitionFieldUtils.Simulations.SwerveDriveSimulation;
import frc.robot.utils.CustomConfigs.MapleConfigFile;
import frc.robot.utils.CustomConfigs.PhotonCameraProperties;
import frc.robot.utils.MapleJoystickDriveInput;
import frc.robot.utils.MapleShooterOptimization;
import org.littletonrobotics.junction.networktables.LoggedDashboardChooser;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

/**
 * This class is where the bulk of the robot should be declared. Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of the robot (including
 * subsystems, commands, and button mappings) should be declared here.
 */
public class RobotContainer {
    // pdp for akit logging
    public final PowerDistribution powerDistribution;
    // Subsystems
    public final SwerveDrive drive;
    public final AprilTagVision aprilTagVision;
    public final LEDStatusLight ledStatusLight;

    /* an example shooter optimization */
    public final MapleShooterOptimization exampleShooterOptimization;

    // Controller
    private final CommandXboxController driverXBox = new CommandXboxController(0);


    public enum JoystickMode {
        LEFT_HANDED,
        RIGHT_HANDED
    }
    // Dashboard Selections
    private final LoggedDashboardChooser<JoystickMode> driverModeChooser;
    private final LoggedDashboardChooser<Auto> autoChooser;
    private final SendableChooser<Supplier<Command>> testChooser;

    // Simulation and Field Visualization
    private final CompetitionFieldVisualizer competitionFieldVisualizer;
    private CompetitionFieldSimulation fieldSimulation;

    /**
     * The container for the robot. Contains subsystems, OI devices, and commands.
     */
    public RobotContainer() {
        final MapleConfigFile chassisCalibrationFile;
        try {
            chassisCalibrationFile = MapleConfigFile.fromDeployedConfig(
                    "ChassisWheelsCalibration",
                    Constants.ROBOT_NAME
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final List<PhotonCameraProperties> camerasProperties = PhotonCameraProperties.loadCamerasPropertiesFromConfig(Constants.ROBOT_NAME);
        final MapleConfigFile.ConfigBlock chassisGeneralConfigBlock = chassisCalibrationFile.getBlock("GeneralInformation");

        this.ledStatusLight = new LEDStatusLight(0, 155);
        switch (Robot.CURRENT_ROBOT_MODE) {
            case REAL -> {
                // Real robot, instantiate hardware IO implementations
                powerDistribution = new PowerDistribution(0, PowerDistribution.ModuleType.kCTRE);
                drive = new SwerveDrive(
                        new GyroIOPigeon2(),
                        new ModuleIOTalonFX(chassisCalibrationFile.getBlock("FrontLeft"), chassisGeneralConfigBlock),
                        new ModuleIOTalonFX(chassisCalibrationFile.getBlock("FrontRight"), chassisGeneralConfigBlock),
                        new ModuleIOTalonFX(chassisCalibrationFile.getBlock("BackLeft"), chassisGeneralConfigBlock),
                        new ModuleIOTalonFX(chassisCalibrationFile.getBlock("BackRight"), chassisGeneralConfigBlock),
                        chassisGeneralConfigBlock
                );

                aprilTagVision = new AprilTagVision(
                        new AprilTagVisionIOReal(camerasProperties),
                        camerasProperties,
                        drive
                );

                this.competitionFieldVisualizer = new CompetitionFieldVisualizer(drive::getPose);
            }

            case SIM -> {
                powerDistribution = new PowerDistribution();
                // Sim robot, instantiate physics sim IO implementations
                final ModuleIOSim
                        frontLeft = new ModuleIOSim(chassisGeneralConfigBlock),
                        frontRight = new ModuleIOSim(chassisGeneralConfigBlock),
                        backLeft = new ModuleIOSim(chassisGeneralConfigBlock),
                        backRight = new ModuleIOSim(chassisGeneralConfigBlock);
                final GyroIOSim gyroIOSim = new GyroIOSim();
                drive = new SwerveDrive(
                        gyroIOSim,
                        frontLeft, frontRight, backLeft, backRight,
                        chassisGeneralConfigBlock
                );
                final SwerveDriveSimulation driveSimulation = new SwerveDriveSimulation(
                        chassisGeneralConfigBlock,
                        gyroIOSim,
                        frontLeft, frontRight, backLeft, backRight,
                        drive.kinematics,
                        new Pose2d(3, 3, new Rotation2d()),
                        drive::setPose
                );
                fieldSimulation = new Crescendo2024FieldSimulation(driveSimulation);
                this.competitionFieldVisualizer = fieldSimulation.getCompetitionField();

                aprilTagVision = new AprilTagVision(
                        new ApriltagVisionIOSim(
                                camerasProperties,
                                Constants.VisionConfigs.fieldLayout,
                                driveSimulation::getObjectOnFieldPose2d
                        ),
                        camerasProperties,
                        drive
                );

                fieldSimulation.placeGamePiecesOnField();

                fieldSimulation.addRobot(new OpponentRobotSimulation(0));
                fieldSimulation.addRobot(new OpponentRobotSimulation(1));
                fieldSimulation.addRobot(new OpponentRobotSimulation(2));
            }

            default -> {
                powerDistribution = new PowerDistribution();
                // Replayed robot, disable IO implementations
                drive = new SwerveDrive(
                        (inputs) -> {},
                        (inputs) -> {},
                        (inputs) -> {},
                        (inputs) -> {},
                        (inputs) -> {},
                        chassisGeneralConfigBlock
                );

                aprilTagVision = new AprilTagVision(
                        (inputs) -> {},
                        camerasProperties,
                        drive
                );

                this.competitionFieldVisualizer = new CompetitionFieldVisualizer(drive::getPose);
            }
        }

        this.drive.configHolonomicPathPlannerAutoBuilder(competitionFieldVisualizer);

        SmartDashboard.putData("Select Test", testChooser = buildTestsChooser());
        autoChooser = buildAutoChooser();

        driverModeChooser = new LoggedDashboardChooser<>("Driver Mode", new SendableChooser<>());
        driverModeChooser.addDefaultOption(JoystickMode.LEFT_HANDED.name(), JoystickMode.LEFT_HANDED);
        driverModeChooser.addOption(JoystickMode.RIGHT_HANDED.name(), JoystickMode.RIGHT_HANDED);

        /* you can tune the numbers on dashboard and copy-paste them to here */
        this.exampleShooterOptimization = new MapleShooterOptimization(
                "ExampleShooter",
                new double[] {1.4, 2, 3, 3.5, 4, 4.5, 4.8},
                new double[] {54, 49, 37, 33.5, 30.5, 25, 25},
                new double[] {3000, 3000, 3500, 3700, 4000, 4300, 4500},
                new double[] {0.1, 0.1, 0.1, 0.12, 0.12, 0.15, 0.15}
        );


        configureButtonBindings();
        configureAutoNamedCommands();
    }

    private static LoggedDashboardChooser<Auto> buildAutoChooser() {
        final LoggedDashboardChooser<Auto> autoSendableChooser = new LoggedDashboardChooser<>("Select Auto");
        autoSendableChooser.addDefaultOption("None", Auto.none());
        autoSendableChooser.addOption("Example Custom Auto", new ExampleAuto());
        autoSendableChooser.addOption("Example Pathplanner Auto", new PathPlannerAuto("Example Auto", new Pose2d(1.3, 7.2, new Rotation2d())));
        SmartDashboard.putData("Select Auto", autoSendableChooser.getSendableChooser());

        // TODO: add your autos here
        return autoSendableChooser;
    }

    private static SendableChooser<Supplier<Command>> buildTestsChooser() {
        final SendableChooser<Supplier<Command>> testsChooser = new SendableChooser<>();
        testsChooser.setDefaultOption("None", Commands::none);
        testsChooser.addOption("Wheels Calibration", WheelsCalibrationCTRE::new);
        // TODO add your tests here (system identification and etc.)
        return testsChooser;
    }

    private boolean isDSPresentedAsRed = Constants.isSidePresentedAsRed();
    private boolean isLeftHanded = true;
    private Command autonomousCommand = Commands.none();
    private Auto previouslySelectedAuto = null;
    /**
     * reconfigures button bindings if alliance station has changed
     * re-create autos if not yet created
     * */
    public void checkForCommandChanges() {
        final boolean isLeftHandedSelected = !JoystickMode.RIGHT_HANDED.equals(driverModeChooser.get());
        if (Constants.isSidePresentedAsRed() != isDSPresentedAsRed || isLeftHanded != isLeftHandedSelected)
            configureButtonBindings();
        isDSPresentedAsRed = Constants.isSidePresentedAsRed();
        isLeftHanded = isLeftHandedSelected;

        final Auto selectedAuto = autoChooser.get();
        if (selectedAuto != previouslySelectedAuto) {
            this.autonomousCommand = selectedAuto
                    .getAutoCommand(this)
                    .beforeStarting(() -> resetFieldAndOdometryForAuto(selectedAuto.getStartingPoseAtBlueAlliance()))
                    .finallyDo(MapleSubsystem::disableAllSubsystems);
        }
        previouslySelectedAuto = selectedAuto;
    }

    private void resetFieldAndOdometryForAuto(Pose2d robotStartingPoseAtBlueAlliance) {
        final Pose2d startingPose = Constants.toCurrentAlliancePose(robotStartingPoseAtBlueAlliance);
        drive.setPose(startingPose);

        if (fieldSimulation == null) return;
        fieldSimulation.getMainRobot().setSimulationWorldPose(startingPose);
        fieldSimulation.resetFieldForAuto();
    }

    /**
     * Use this method to define your button->command mappings. Buttons can be created by
     * instantiating a {@link GenericHID} or one of its subclasses ({@link
     * Joystick} or {@link XboxController}), and then passing it to a {@link
     * JoystickButton}.
     */
    public void configureButtonBindings() {
        System.out.println("configuring key bindings...  mode:" + driverModeChooser.get());
        final MapleJoystickDriveInput driveInput = JoystickMode.RIGHT_HANDED.equals(driverModeChooser.get()) ?
                MapleJoystickDriveInput.rightHandedJoystick(driverXBox)
                : MapleJoystickDriveInput.leftHandedJoystick(driverXBox);

        final JoystickDrive joystickDrive = new JoystickDrive(
                driveInput,
                () -> true,
                drive
        );
        drive.setDefaultCommand(joystickDrive);
        driverXBox.x().whileTrue(Commands.run(drive::lockChassisWithXFormation, drive));
        driverXBox.start().onTrue(Commands.runOnce(
                        () -> drive.setPose(new Pose2d(drive.getPose().getTranslation(), new Rotation2d())),
                        drive
                ).ignoringDisable(true)
        );

        /* aim at target and drive example, delete it for your project */
        final JoystickDriveAndAimAtTarget exampleFaceTargetWhileDriving = new JoystickDriveAndAimAtTarget(
                driveInput, drive,
                Constants.CrescendoField2024Constants.SPEAKER_POSITION_SUPPLIER,
                exampleShooterOptimization,
                0.5
        );
        driverXBox.rightTrigger(0.5).whileTrue(exampleFaceTargetWhileDriving);

        /* auto alignment example, delete it for your project */
        final AutoAlignment exampleAutoAlignment = new AutoAlignment(
                drive,
                /* (position of AMP) */
                () -> Constants.toCurrentAlliancePose(new Pose2d(1.85, 7.6, Rotation2d.fromDegrees(90)))
        );
    }

    private void configureAutoNamedCommands() {
        // bind your named commands during auto here
        NamedCommands.registerCommand("my named command", Commands.runOnce(
                () -> System.out.println("my named command executing!!!")
        ));
    }

    /**
     * Use this to pass the autonomous command to the main {@link Robot} class.
     *
     * @return the command to run in autonomous
     */
    public Command getAutonomousCommand() {
        return autonomousCommand;
    }

    public Command getTestCommand() {
        return testChooser.getSelected().get();
    }

    public void updateFieldSimAndDisplay() {
        if (fieldSimulation != null)
            fieldSimulation.updateSimulationWorld();

        competitionFieldVisualizer.updateObjectsToDashboardAndTelemetry();
    }
}
