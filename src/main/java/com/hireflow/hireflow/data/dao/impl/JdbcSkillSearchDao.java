package com.hireflow.hireflow.data.dao.impl;

import com.hireflow.hireflow.data.dao.SkillSearchDao;
import com.hireflow.hireflow.dto.response.SkillResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcSkillSearchDao implements SkillSearchDao {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<SkillResponse> searchByNamePrefix(String prefix) {
        String sql = """
                SELECT id, name
                FROM skills
                WHERE LOWER(name) LIKE CONCAT(LOWER(?), '%')
                ORDER BY name ASC
                """;

        return jdbcTemplate.query(sql, this::mapSkillResponse, prefix);
    }

    private SkillResponse mapSkillResponse(ResultSet rs, int rowNum) throws SQLException {
        SkillResponse response = new SkillResponse();
        response.setId(rs.getString("id"));
        response.setName(rs.getString("name"));
        return response;
    }
}
