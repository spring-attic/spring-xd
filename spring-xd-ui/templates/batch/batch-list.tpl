<table class="table table-bordered table-striped info-table" id="batch">
	<thead>
		<tr>
			<td>Job name</td>
			<td>Description</td>
			<td>Execution count</td>
			<td>Actions</td>
		</tr>
	</thead>
	<tbody>
    <!-- requires: name, description, executionCount, isLaunchable -->
    	<% jobs.forEach(function(job) {
    	   var details = job.name + '_details', detailsRow = job.name + '_detailsRow'; %>
        <tr id="<%= name %>">
    		<td>
    		  <button class="btn detailAction" type="button" job-name="<%= job.name %>" data-toggle="collapse" data-target="#<%= details %>">
    		    <strong><a><%= job.name %></a>
    		  </strong></button>
    		</td>
    		<td><%= job.description %></td>
    		<td><%= job.executionCount %></td>
    		<td>
    		  <button class="btn btn-mini launchAction <%=job.isLaunchable ? '' : 'disabled'%>" job-name="<%= job.name %>" type="button">Launch</button>
    	    </td>
    	</tr>
    	<tr id="<%= detailsRow %>"><td colspan="4"><div id="<%= details %>" class="collapse in"></div></td></tr>
        <% }); %>
	</tbody>
</table>
