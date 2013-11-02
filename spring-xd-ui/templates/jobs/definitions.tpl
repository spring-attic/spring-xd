<div class="col-md-12 table-responsive">
	<table class="table table-hover">
		<thead>
			<tr>
				<th>Name</th>
				<th>Deploy</th>
				<th>UnDeploy</th>
				<th>Definition</th>
			</tr>
			<tr>
			</tr>
			</thead>
			<tbody>
				<% for (var i in jobs) { %>
				<% if (jobs[i].deployed == true) { %>
				<tr class="success">
				<% } else { %>
				<tr class="warning">
				<% } %>
					<% if (jobs[i].deployed == true) { %>
						<td><%= jobs[i].name %></td>
						<td><button type="button" class="btn btn-default btn-sm job-deploy disabled" id="deploy-<%= jobs[i].name %>">Deploy</button></td>
						<td><button type="button" class="btn btn-default btn-sm job-undeploy" id="undeploy-<%= jobs[i].name %>">Undeploy</button></td>
					<% } else { %>
						<td><%= jobs[i].name %></td>
						<td><button type="button" class="btn btn-default btn-sm job-deploy" id="deploy-<%= jobs[i].name %>">Deploy</button></td>
						<td><button type="button" class="btn btn-default btn-sm job-undeploy disabled" id="undeploy-<%= jobs[i].name %>">Undeploy</button></td>
					<% } %>
					<td><%= jobs[i].definition %></td>
				</tr>
				<% } %>
			</tbody>
		</table>
</div>
