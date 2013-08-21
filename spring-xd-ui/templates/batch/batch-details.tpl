<h3>Instances for job <%= name %></h3>

<table class="table table-bordered table-striped info-table" id="batch-instances-<%= name %>">
	<thead>
		<tr>
			<td>ID</td>
			<td>Execution count</td>
			<td>Last Execution</td>
			<td>Parameters</td>
		</tr>
	</thead>
	<tbody>
	<% Object.keys(jobInstances).forEach(function(key) { %>
	    <td><%= key %></td>
	    <td><%= jobInstances[key].executionCount %></td>
	    <td<%= jobInstances[key].lastJobExecutionStatus === 'FAILURE' ? ' class="text-danger"' : '' %> >
	       <strong><%= jobInstances[key].lastJobExecutionStatus %></strong></td>
	    <td><%= JSON.stringify(jobInstances[key].jobParameters) %></td>
	<% }); %>
	</tbody>
</table>