package frc.robot.tests;

import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.MecanumDriveKinematics;
import edu.wpi.first.math.kinematics.MecanumDriveWheelSpeeds;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.utils.MapleJoystickDriveInput;

public class MecanumKinematicsTest implements UnitTest {
    private final MecanumDriveKinematics kinematics = new MecanumDriveKinematics(
            new Translation2d(0.3, 0.3),
            new Translation2d(0.3, -0.3),
            new Translation2d(-0.3, 0.3),
            new Translation2d(-0.3, -0.3)
    );
    private final MapleJoystickDriveInput driveInput = MapleJoystickDriveInput.leftHandedJoystick(new XboxController(1));
    @Override
    public void testStart() {

    }

    @Override
    public void testPeriodic() {
        final MecanumDriveWheelSpeeds wheelSpeeds = kinematics.toWheelSpeeds(driveInput.getJoystickChassisSpeeds(1, 1));
        System.out.println("input chassis speeds");
        SmartDashboard.putNumber("frontLeft", wheelSpeeds.frontLeftMetersPerSecond);
        SmartDashboard.putNumber("frontRight", wheelSpeeds.frontRightMetersPerSecond);
        SmartDashboard.putNumber("backLeft", wheelSpeeds.rearLeftMetersPerSecond);
        SmartDashboard.putNumber("backRight", wheelSpeeds.rearRightMetersPerSecond);
    }
}
