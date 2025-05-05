package server.data;

import server.models.Point;

import java.sql.*;
import java.util.ArrayList;


public class DataBase {
    private static final String URL = "jdbc:postgresql://localhost:5432/studs";
    private static final String USER = "mike-yasnov";
    private static final String PASSWORD = "20348722";





    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void createPointsTable(Connection connection) {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS points ("
                + "ID SERIAL PRIMARY KEY, "
                + "res BOOLEAN NOT NULL,"
                + "x REAL NOT NULL, "
                + "y REAL NOT NULL,"
                + "r REAL NOT NULL, "
                + "dateOfRequest VARCHAR(255) NOT NULL,"
                + "executionTime REAL NOT NULL"
                + ");";
        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void insertPoint(Connection connection, boolean res ,double x , double y , double r ,String dateOfRequest , long executionTime) {
        String insertSQL = "INSERT INTO points (res,x, y, r,dateOfRequest , executionTime) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
            preparedStatement.setBoolean(1, res);
            preparedStatement.setDouble(2, x);
            preparedStatement.setDouble(3, y);
            preparedStatement.setDouble(4, r);
            preparedStatement.setString(5, dateOfRequest);
            preparedStatement.setLong(6, executionTime);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Point> getLastPoints(Connection connection) {
        ArrayList<Point> points = new ArrayList<Point>();
        String query = "SELECT * FROM points ORDER BY id DESC LIMIT 5";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                boolean res = resultSet.getBoolean("res");
                double x = resultSet.getDouble("x");
                double y = resultSet.getDouble("y");
                double r = resultSet.getDouble("r");
                String dateOfRequest = resultSet.getString("dateOfRequest");
                long executionTime = resultSet.getLong("executionTime");

                Point point = new Point(res,x, y, r, dateOfRequest, executionTime);
                points.add(point);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return points;
    }

}
