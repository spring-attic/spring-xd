<div class="col-md-12 table-responsive">
		<table class="table table-hover">
			<thead>
			<tr>
				<th>Name</th>
				<th>Launch</th>
				<th>Schedule</th>
				<th>Execution Count</th>
				<th>Last Execution Status</th>
			</tr>
			<tr>
			</tr>
			</thead>
			<tbody>
				<% for (var i in jobs) { %>
					<tr>
						<td><%= jobs[i].name %></td>
						<% if (jobs[i].launchable === true) { %>
								<td><button type="button" data-toggle="modal" data-target="#job-params-modal" class="btn btn-default open-job-params" id="launch-<%= jobs[i].name %>">Launch</button></td>
								<td><button type="button" class="btn btn-default job-schedule" id="schedule-<%= jobs[i].name %>">Schedule</button></td>
						<% } else { %>
								<td></td>
								<td></td>
						<% } %>
						<td><%= jobs[i].executionCount %></td>
						<% if (jobs[i].exitStatus != null) { %>
							<td><%= jobs[i].exitStatus.exitCode %></td>
						<% } else { %>
							<td></td>
						<% } %>
					</tr>
				<% } %>
			</tbody>
		</table>
</div>
<div class="modal" id="job-params-modal" tabindex="-1" role="dialog" aria-labelledby="job-launch-label" aria-hidden="true">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
        <h4 class="modal-title" id="job-launch-label">Job Parameters</h4>
      </div>
      <div class="modal-body" id="job-params-form">
		<form class="form-inline" role="form">
			<div class="form-group">
				<input type="text" class="form-control" placeholder="key">
			</div>
			<div class="form-group">
				<input type="text" class="form-control" placeholder="value">
			</div>
			<button type="button" class="btn btn-small add-job-param">Add Param</button>
		</form>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
        <button type="button" class="btn btn-primary">Launch Job</button>
      </div>
    </div>
  </div>
</div>