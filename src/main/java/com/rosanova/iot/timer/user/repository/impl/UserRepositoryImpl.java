package com.rosanova.iot.timer.user.repository.impl;

import com.rosanova.iot.timer.user.User;
import com.rosanova.iot.timer.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ✅ Insert a new user
    public void insertUser(User user) {
        String sql = "INSERT INTO user_timer (user_name, password) VALUES (?, ?)";
        jdbcTemplate.update(sql, user.getUsername(), user.getPassword());
    }

    // ✅ Delete a user by ID
    @CacheEvict(value = "users", allEntries = true)
    public void deleteUserById(long id) {
        String sql = "DELETE FROM user_timer WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }


    public User getByUsername(String username) {
        String sql = "SELECT * FROM user_timer WHERE user_name = ?";
        List<User> result = jdbcTemplate.query(sql, (ResultSet rs, int rs2) -> mapToUser(rs), username);
        return result.isEmpty() ? null : result.get(0);
    }

    // ✅ Retrieve all users
    @Cacheable(value = "users")
    public List<User> findAllUsers() {
        String sql = "SELECT * FROM user_timer";
        return jdbcTemplate.query(sql, this::mapRowToUser);
    }

    // ✅ Find user by ID
    @Cacheable(value = "users", key = "#id")
    public User findById(long id) {
        String sql = "SELECT * FROM user_timer WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, (ResultSet rs, int rs2) -> mapToUser(rs), id);
    }

    // 🔧 Helper to map ResultSet to User
    private User mapRowToUser(ResultSet rs, int row) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("user_name"));
        user.setPassword(rs.getString("password"));
        return user;
    }

    // 🔧 Helper to map ResultSet to User
    private User mapToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("user_name"));
        user.setPassword(rs.getString("password"));
        return user;
    }
}
