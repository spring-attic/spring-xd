package org.springframework.xd.test.fixtures;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Represents the {@code jdbc} source module. Embeds a datasource to which
 * data can be added, so that it gets picked up by the module.
 *
 * @author Eric Bottard
 */
public class JdbcSource extends AbstractModuleFixture<JdbcSink> implements Disposable {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private String update;
    private String query;
    private int fixedDelay = 5;

    public JdbcSource(DataSource dataSource) {

        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @Override
    protected String toDSL() {
        String result = null;
        try {
            result = String.format("jdbc --query='%s' --url=%s --fixedDelay=%d", shellEscape(query), dataSource.getConnection().getMetaData().getURL(), fixedDelay);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        if (update != null) {
            result += String.format(" --update='%s'", shellEscape(update));
        }
        return result;

    }

    /**
     * Escapes quotes for the XD shell.
     */
    private String shellEscape(String sql) {
        return sql.replace("'", "''");
    }

    @Override
    public void cleanup() {
        jdbcTemplate.execute("SHUTDOWN");
    }

    public JdbcSource update(String update) {
        this.update = update;
        return this;
    }

    public JdbcSource query(String query) {
        this.query = query;
        return this;
    }

    public JdbcSource fixedDelay(int delay) {
        this.fixedDelay = delay;
        return this;
    }
}
