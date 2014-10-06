package org.springframework.xd.jdbc;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;

import javax.validation.constraints.Min;

/**
 * Captures options for the {@code jdbc} source module.
 *
 * @author Eric Bottard
 */
@Mixin({JdbcConnectionMixin.class, JdbcConnectionPoolMixin.class})
public class JdbcSourceModuleOptionsMetadata implements ProfileNamesProvider {

    private static final String[] USE_SPLITTER = new String[]{"use-splitter"};
    private static final String[] DONT_USE_SPLITTER = new String[]{"dont-use-splitter"};


    private String query;

    private String update;

    private int fixedDelay = 5;

    private boolean split = true;

    @NotBlank
    public String getQuery() {
        return query;
    }

    @ModuleOption("an SQL select query to execute to retrieve new messages when polling")
    public void setQuery(String query) {
        this.query = query;
    }

    public String getUpdate() {
        return update;
    }

    @ModuleOption("an SQL update statement to execute for marking polled messages as 'seen'")
    public void setUpdate(String update) {
        this.update = update;
    }

    @Min(1)
    public int getFixedDelay() {
        return fixedDelay;
    }

    @ModuleOption("how often to poll for new messages (s)")
    public void setFixedDelay(int fixedDelay) {
        this.fixedDelay = fixedDelay;
    }

    public boolean isSplit() {
        return split;
    }

    @ModuleOption("whether to split the SQL result as individual messages")
    public void setSplit(boolean split) {
        this.split = split;
    }

    @Override
    public String[] profilesToActivate() {
        return split ? USE_SPLITTER : DONT_USE_SPLITTER;
    }
}
