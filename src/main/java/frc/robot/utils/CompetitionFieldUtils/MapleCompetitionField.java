package frc.robot.utils.CompetitionFieldUtils;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.utils.CompetitionFieldUtils.FieldObjects.Crescendo2024.RobotOnField;
import org.littletonrobotics.junction.Logger;

import java.util.*;

/**
 * stores and displays a competition field
 * that includes the robot, the opponent robots and game pieces on field
 * notice that this class only stores and displays the field information to dashboard/advantage scope
 * it does not update the field status
 * the field should be updated either by the vision system during a real competition or by the Maple Physics Simulation during a simulated competition
 * */
public class MapleCompetitionField {
    public interface ObjectOnField {
        String getTypeName();
        Pose3d getPose3d();
        default boolean on2dField() {return false; }
    }

    public interface ObjectOn2dField extends ObjectOnField {
        Pose2d getPose2d();
        @Override
        String getTypeName();
        @Override
        default Pose3d getPose3d() {
            return new Pose3d(getPose2d());
        }

        @Override
        default boolean on2dField() {return true; }
    }

    private final Map<String, Set<ObjectOnField>> objectsOnFieldWithGivenType;
    private final RobotOnField robot;
    private final Field2d dashboardField2d;
    public MapleCompetitionField(RobotOnField robot) {
        this.robot = robot;
        objectsOnFieldWithGivenType = new HashMap<>();
        dashboardField2d = new Field2d();
        SmartDashboard.putData("Field", dashboardField2d);
    }

    public ObjectOnField addObject(ObjectOnField object) {
        if (!objectsOnFieldWithGivenType.containsKey(object.getTypeName()))
            objectsOnFieldWithGivenType.put(object.getTypeName(), new HashSet<>());
        objectsOnFieldWithGivenType.get(object.getTypeName()).add(object);
        return object;
    }

    public ObjectOnField deleteObject(ObjectOnField object) {
        if (!objectsOnFieldWithGivenType.containsKey(object.getTypeName()))
            return null;
        if (objectsOnFieldWithGivenType.get(object.getTypeName()).remove(object))
            return object;
        return null;
    }

    public Set<ObjectOnField> clearObjectsWithGivenType(String typeName) {
        if (!objectsOnFieldWithGivenType.containsKey(typeName))
            return new HashSet<>();
        final Set<ObjectOnField> originalSet = objectsOnFieldWithGivenType.get(typeName);
        objectsOnFieldWithGivenType.put(typeName, new HashSet<>());
        return originalSet;
    }

    public void updateObjectsToDashboardAndTelemetry() {
        for (String typeName: objectsOnFieldWithGivenType.keySet()) {
            final Set<ObjectOnField> objects = objectsOnFieldWithGivenType.get(typeName);
            dashboardField2d.getObject(typeName).setPoses(getPose2ds(objects));
            Logger.recordOutput("/Field/" + typeName, getPose3ds(objects));
        }

        dashboardField2d.setRobotPose(robot.getPose2d());
        Logger.recordOutput("/Field/Robot", robot.getPose2d());
    }

    private static List<Pose2d> getPose2ds(Set<ObjectOnField> objects) {
        final List<Pose2d> pose2dList = new ArrayList<>();

        for (ObjectOnField object:objects)
            if (object.on2dField())
                pose2dList.add(object.getPose3d().toPose2d());
        return pose2dList;
    }

    private static Pose3d[] getPose3ds(Set<ObjectOnField> objects) {
        return objects.stream().map(ObjectOnField::getPose3d).toArray(Pose3d[]::new);
    }
}
