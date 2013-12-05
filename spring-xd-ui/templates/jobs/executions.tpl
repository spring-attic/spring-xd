<div class="col-md-12 table-responsive">
		<table class="table table-hover">
			<thead>
			<tr>
				<th>Id</th>
				<th>Job</th>
				<th>Start Time</th>
				<th>Step Executions Count</th>
				<th>Status</th>
			</tr>
			<tr>
			</tr>
			</thead>
			<tbody>
				<% for (var i in executions) { %>
					<tr>
						<td><%= executions[i].executionId %></td>
						<td><%= executions[i].name %></td>
						<td><%= executions[i].startDate + ' ' + executions[i].startTime +  ' ' + executions[i].timeZone %></td>
						<td><%= executions[i].stepExecutionCount %></td>
						<% if (executions[i].jobExecution != null) { %>
							<td><%= executions[i].jobExecution.exitStatus.exitCode %> </td>
						<% } else { %>
							<td></td>
						<% } %>
					</tr>
				<% } %>
			</tbody>
		</table>
</div>
