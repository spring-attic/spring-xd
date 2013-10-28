<div class="col-md-12 table-responsive">
	<table class="table table-hover">
		<thead>
			<tr>
				<th>Name</th>
				<th>Status</th>
				<th>Definition</th>
				<th>Deploy</th>
				<th>UnDeploy</th>
			</tr>
			<tr>
			</tr>
			</thead>
			<tbody>
				<% for (var i in jobs) { %>
				<tr>
					<td><%= jobs[i].name %></td>
					<td>TBD</td>
					<td><%= jobs[i].definition %></td>
					<td><button type="button" class="btn btn-default btn-sm">Deploy</button></td>
					<td><button type="button" class="btn btn-default btn-sm disabled">UnDeploy</button></td>
				</tr>
				<% } %>
			</tbody>
		</table>
</div>
