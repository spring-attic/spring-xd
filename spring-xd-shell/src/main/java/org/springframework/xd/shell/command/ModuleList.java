package org.springframework.xd.shell.command;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.springframework.util.Assert;
import org.springframework.xd.rest.client.domain.ModuleDefinitionResource;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;

import java.util.Collection;
import java.util.Iterator;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Multimaps.index;

/**
 * Knows how to render a {@link Table} of {@link ModuleDefinitionResource}.
 *
 * @author Florent Biville
 */
class ModuleList {

    private static final Function<ModuleDefinitionResource,String> BY_TYPE = new Function<ModuleDefinitionResource, String>() {
        @Override
        public String apply(ModuleDefinitionResource input) {
            return input.getType();
        }
    };

    private final Multimap<String,ModuleDefinitionResource> modulesByType;

    public ModuleList(Iterable<ModuleDefinitionResource> modules) {
        Assert.state(modules != null);
        modulesByType = TreeMultimap.create(index(modules, BY_TYPE));
    }

    public Table renderByType() {
        final Table table = new Table();

        initializeHeader(table);
        for (int i = 0; i < computeRowCount(); i++) {
            table.addRow(computeRowValues(i));
        }
        return table;
    }

    private void initializeHeader(Table table) {
        int columnIndex = 1;
        for (String type : modulesByType.keySet()) {
            table.addHeader(columnIndex++, new TableHeader(type));
        }
    }

    private int computeRowCount() {
        int maxRows = 0;
        for (String type : modulesByType.keySet()) {
            int typeRows = modulesByType.get(type).size();
            if (typeRows > maxRows) {
                maxRows = typeRows;
            }
        }
        return maxRows;
    }

    private String[] computeRowValues(int i) {
        String[] valueArray = new String[modulesByType.keys().size()];
        return computeRowValueCollection(i).toArray(valueArray);
    }

    private Collection<String> computeRowValueCollection(int lineNumber) {
        Collection<String> rowValues = newLinkedList();
        for (String type : modulesByType.keySet()) {
            Iterator<ModuleDefinitionResource> modules = modulesByType.get(type).iterator();
            ModuleDefinitionResource module = Iterators.get(modules, lineNumber, null);
            rowValues.add(computeModuleValue(module));
        }
        return rowValues;
    }

    private String computeModuleValue(ModuleDefinitionResource module) {
        if (module == null) {
            return "";
        }
        return module.getName();
    }
}
