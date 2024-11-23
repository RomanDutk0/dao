package com.epam.rd.autocode.dao;
import com.epam.rd.autocode.ConnectionSource;
import com.epam.rd.autocode.domain.Department;
import com.epam.rd.autocode.domain.Employee;
import com.epam.rd.autocode.domain.FullName;
import com.epam.rd.autocode.domain.Position;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class DaoFactory {
    private static final String DB_URL = "jdbc:your_database_url";
    private static final String DB_USER = "your_database_user";
    private static final String DB_PASSWORD = "your_database_password";

    private Connection getConnection() {
        try {
            return ConnectionSource.instance().createConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Error connecting to the database", e);
        }
    }

    public EmployeeDao employeeDAO() {
        return new EmployeeDaoImpl(getConnection());
    }

    public DepartmentDao departmentDAO() {
        return new DepartmentDaoImpl(getConnection());
    }

    // Inner class for EmployeeDao
    private class EmployeeDaoImpl implements EmployeeDao {
        private final Connection connection;

        EmployeeDaoImpl(Connection connection) {
            this.connection = connection;
        }

        @Override
        public Optional<Employee> getById(BigInteger id) {
            String query = "SELECT * FROM EMPLOYEE WHERE ID = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setBigDecimal(1, new BigDecimal(id));
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    return Optional.of(mapRowToEmployee(rs));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return Optional.empty();
        }

        @Override
        public List<Employee> getAll() {
            String query = "SELECT ID, FIRSTNAME, LASTNAME, MIDDLENAME, POSITION, HIREDATE, SALARY, MANAGER, DEPARTMENT FROM EMPLOYEE";
            List<Employee> employees = new ArrayList<>();

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(query)) {

                while (rs.next()) {
                    BigInteger employeeId = rs.getBigDecimal("ID").toBigInteger();
                    String firstName = rs.getString("FIRSTNAME");
                    String lastName = rs.getString("LASTNAME");
                    String middleName = rs.getString("MIDDLENAME");
                    FullName fullName = new FullName(firstName, lastName, middleName);
                    Position position = Position.valueOf(rs.getString("POSITION"));
                    LocalDate hired = rs.getDate("HIREDATE").toLocalDate(); // Замінено "HIRED" на "HIRE_DATE"
                    BigDecimal salary = rs.getBigDecimal("SALARY");
                    BigInteger managerId = rs.getBigDecimal("MANAGER") != null ? rs.getBigDecimal("MANAGER").toBigInteger() : BigInteger.ZERO;
                    BigInteger departmentId = rs.getBigDecimal("DEPARTMENT") != null ? rs.getBigDecimal("DEPARTMENT").toBigInteger() : BigInteger.ZERO;
                    Employee employee = new Employee(employeeId, fullName, position, hired, salary, managerId, departmentId);
                    employees.add(employee);
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error retrieving employees", e);
            }

            return employees;
        }




        @Override
        public Employee save(Employee employee) {
            String checkQuery = "SELECT COUNT(*) FROM EMPLOYEE WHERE ID = ?";
            String updateQuery = "UPDATE EMPLOYEE SET FIRSTNAME = ?, LASTNAME = ?, MIDDLENAME = ?, POSITION = ?, " +
                    "HIREDATE = ?, SALARY = ?, MANAGER = ?, DEPARTMENT = ? WHERE ID = ?";
            String insertQuery = "INSERT INTO EMPLOYEE (ID, FIRSTNAME, LASTNAME, MIDDLENAME, POSITION, HIREDATE, SALARY, MANAGER, DEPARTMENT) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try {
                // Перевірка наявності запису
                try (PreparedStatement checkStatement = connection.prepareStatement(checkQuery)) {
                    checkStatement.setBigDecimal(1, new BigDecimal(employee.getId()));
                    try (ResultSet rs = checkStatement.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            // Якщо запис існує, виконуємо UPDATE
                            try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                                FullName fullName = employee.getFullName();
                                // Логування значень перед збереженням
                                updateStatement.setString(1, fullName.getFirstName());
                                updateStatement.setString(2, fullName.getLastName());
                                updateStatement.setString(3, fullName.getMiddleName());
                                updateStatement.setString(4, employee.getPosition().name());
                                updateStatement.setDate(5, Date.valueOf(employee.getHired()));
                                updateStatement.setBigDecimal(6, employee.getSalary());
                                updateStatement.setBigDecimal(7, employee.getManagerId() != null
                                        ? new BigDecimal(employee.getManagerId()) : null);
                                updateStatement.setBigDecimal(8, employee.getDepartmentId() != null
                                        ? new BigDecimal(employee.getDepartmentId()) : null);
                                updateStatement.setBigDecimal(9, new BigDecimal(employee.getId()));
                                updateStatement.executeUpdate();
                                return employee;
                            }
                        }
                    }
                }

                // Якщо запису немає, виконуємо INSERT
                try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                    FullName fullName = employee.getFullName();
                    // Логування значень перед збереженням
                    insertStatement.setBigDecimal(1, new BigDecimal(employee.getId()));
                    insertStatement.setString(2, fullName.getFirstName());
                    insertStatement.setString(3, fullName.getLastName());
                    insertStatement.setString(4, fullName.getMiddleName());
                    insertStatement.setString(5, employee.getPosition().name());
                    insertStatement.setDate(6, Date.valueOf(employee.getHired()));
                    insertStatement.setBigDecimal(7, employee.getSalary());
                    insertStatement.setBigDecimal(8, employee.getManagerId() != null
                            ? new BigDecimal(employee.getManagerId()) : null);
                    insertStatement.setBigDecimal(9, employee.getDepartmentId() != null
                            ? new BigDecimal(employee.getDepartmentId()) : null);
                    insertStatement.executeUpdate();
                    return employee;
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error saving employee", e);
            }
        }



        @Override
        public void delete(Employee employee) {
            String query = "DELETE FROM EMPLOYEE WHERE ID = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setBigDecimal(1, new BigDecimal(employee.getId()));
                statement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public List<Employee> getByDepartment(Department department) {
            String query = "SELECT * FROM EMPLOYEE WHERE DEPARTMENT = ?";
            List<Employee> employees = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setBigDecimal(1, new BigDecimal(department.getId()));
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    employees.add(mapRowToEmployee(rs));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            // Sort the list of employees by last name, then by first name, and middle name
            employees.sort(Comparator.comparing((Employee e) -> e.getFullName().getLastName())
                    .thenComparing(e -> e.getFullName().getFirstName())
                    .thenComparing(e -> e.getFullName().getLastName()));
                   // .thenComparing(e -> e.getFullName().getMiddleName()));

            return employees;
        }




        @Override
        public List<Employee> getByManager(Employee manager) {
            String query = "SELECT * FROM EMPLOYEE WHERE MANAGER = ?";
            List<Employee> employees = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setBigDecimal(1, new BigDecimal(manager.getId()));
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    employees.add(mapRowToEmployee(rs));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return employees;
        }

        private Employee mapRowToEmployee(ResultSet rs) throws SQLException {
            // Mapping full name columns
            FullName fullName = new FullName(
                    rs.getString("FIRSTNAME"),
                    rs.getString("LASTNAME"),
                    rs.getString("MIDDLENAME")
            );

            // Mapping Employee fields
            return new Employee(
                    // Assuming "ID" is an integer, convert it to BigInteger
                    BigInteger.valueOf(rs.getInt("ID")),

                    fullName,

                    // Mapping position enum (ensure Position enum has appropriate values matching the database)
                    Position.valueOf(rs.getString("POSITION")),

                    // Mapping the hired date to LocalDate
                    rs.getDate("HIREDATE").toLocalDate(),

                    // Mapping salary as BigDecimal
                    rs.getBigDecimal("SALARY"),

                    // Manager ID (nullable, using null for empty values)
                    rs.getInt("MANAGER") != 0 ? BigInteger.valueOf(rs.getInt("MANAGER")) : BigInteger.ZERO,

                    // Department ID (nullable, using null for empty values)
                    rs.getInt("DEPARTMENT") != 0 ? BigInteger.valueOf(rs.getInt("DEPARTMENT")) : BigInteger.ZERO

            );


        }


    }

    // Inner class for DepartmentDao
    private class DepartmentDaoImpl implements DepartmentDao {
        private final Connection connection;

        DepartmentDaoImpl(Connection connection) {
            this.connection = connection;
        }

        @Override
        public Optional<Department> getById(BigInteger id) {
            String query = "SELECT * FROM DEPARTMENT WHERE ID = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setBigDecimal(1, new BigDecimal(id));
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    return Optional.of(mapRowToDepartment(rs));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return Optional.empty();
        }

        @Override
        public List<Department> getAll() {
            String query = "SELECT ID, NAME, LOCATION FROM DEPARTMENT"; // SQL запит для отримання всіх відділів
            List<Department> departments = new ArrayList<>();

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(query)) {

                while (rs.next()) {
                    // Отримання значень зі стовпців бази даних
                    BigInteger departmentId = rs.getBigDecimal("ID").toBigInteger(); // Використовуємо BigInteger для ідентифікатора
                    String departmentName = rs.getString("NAME");
                    String location = rs.getString("LOCATION");

                    // Створюємо новий об'єкт Department
                    Department department = new Department(departmentId, departmentName, location);

                    // Додаємо відділ до списку
                    departments.add(department);
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error retrieving departments", e);
            }

            return departments; // Повертаємо список всіх відділів
        }






        @Override
    public Department save(Department department) {
        String checkQuery = "SELECT COUNT(*) FROM DEPARTMENT WHERE ID = ?";
        String updateQuery = "UPDATE DEPARTMENT SET NAME = ?, LOCATION = ? WHERE ID = ?";
        String insertQuery = "INSERT INTO DEPARTMENT (ID, NAME, LOCATION) VALUES (?, ?, ?)";

        try {
            // Перевіряємо, чи існує запис із таким ID
            try (PreparedStatement checkStatement = connection.prepareStatement(checkQuery)) {
                checkStatement.setBigDecimal(1, new BigDecimal(department.getId()));
                try (ResultSet rs = checkStatement.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        // Якщо запис існує, виконуємо UPDATE
                        try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                            updateStatement.setString(1, department.getName());
                            updateStatement.setString(2, department.getLocation());
                            updateStatement.setBigDecimal(3, new BigDecimal(department.getId()));
                            updateStatement.executeUpdate();
                            return department;
                        }
                    }
                }
            }

            // Якщо запису не існує, виконуємо INSERT
            try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                insertStatement.setBigDecimal(1, new BigDecimal(department.getId()));
                insertStatement.setString(2, department.getName());
                insertStatement.setString(3, department.getLocation());
                insertStatement.executeUpdate();
                return department;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving department", e);
        }
    }

        @Override
        public void delete(Department department) {
            String query = "DELETE FROM DEPARTMENT WHERE ID = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setBigDecimal(1, new BigDecimal(department.getId()));
                statement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

    private Department mapRowToDepartment(ResultSet rs) throws SQLException {
        String location = rs.getString("LOCATION");
        return new Department(
            rs.getBigDecimal("ID").toBigInteger(),
            rs.getString("NAME"),
            location != null ? location : "Unknown" // Handle nulls if needed
        );
}

    }
}
