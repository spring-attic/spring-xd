package org.springframework.xd.shell.command;

import com.google.common.base.Function;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.xd.rest.client.domain.ModuleDefinitionResource;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;
import org.springframework.xd.shell.util.TableRow;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.transformValues;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class ModuleListTest {

    @Test(expected = IllegalStateException.class)
    public void failsIfModuleCollectionIsNull() {
        new ModuleList(null);
    }

    @Test
    public void rendersTableByType() {
        Table modules = new ModuleList(newArrayList(
            resource("jms", "source"),
            resource("aggregator", "processor"),
            resource("avro", "sink"),
            resource("file", "source")
        )).renderByType();

        Map<Integer,String> header = headerNames(modules.getHeaders());
        assertThat(header.size(), equalTo(3));
        assertThat(header, Matchers.allOf(
            hasEntry(1, "processor"),
            hasEntry(2, "sink"),
            hasEntry(3, "source")
        ));

        List<TableRow> rows = modules.getRows();
        assertThat(rows, hasSize(2));

        Iterator<TableRow> iterator = rows.iterator();
        TableRow firstRow = iterator.next();
        assertThat(firstRow.getValue(1), equalTo("aggregator"));
        assertThat(firstRow.getValue(2), equalTo("avro"));
        assertThat(firstRow.getValue(3), equalTo("file"));

        TableRow secondRow = iterator.next();
        assertThat(secondRow.getValue(1), equalTo(""));
        assertThat(secondRow.getValue(2), equalTo(""));
        assertThat(secondRow.getValue(3), equalTo("jms"));
    }



    private ModuleDefinitionResource resource(String name, String type) {
        return new ModuleDefinitionResource(name, type);
    }

    private Map<Integer, String> headerNames(Map<Integer, TableHeader> headers) {
        return transformValues(headers, new Function<TableHeader, String>() {
            @Override
            public String apply(TableHeader header) {
                return header.getName();
            }
        });
    }

    private TableRow row(String... values) {
        TableRow row = new TableRow();
        int i = 1;
        for (String value : values) {
            row.addValue(i++, value);
        }
        return row;
    }

}