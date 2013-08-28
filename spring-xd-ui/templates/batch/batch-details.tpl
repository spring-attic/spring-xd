<h5>Instances for job <%= name %></h5>

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
	<% jobInstances.models.forEach(function(jobModel) { 
	   var jobInstance = jobModel.attributes;
	   var alertClass;
	   switch(jobInstance.lastJobExecutionStatus) {
	       case 'FAILED':
	           alertClass = 'alert alert-error';
	           break;
	       case 'STARTED':
	           alertClass = 'alert alert-alert';
	           break;
	       case 'SUCCESS':
	           alertClass = 'alert alert-success';
	           break;
	   }
	%><tr>
	    <td><%= jobInstance.id %></td>
	    <td><%= jobInstance.executionCount %></td>
	    <td><div class="<%= alertClass %>">
	       <%= jobInstance.lastJobExecutionStatus %>
	    </div></td>
	    <td><%= JSON.stringify(jobInstance.jobParameters) %></td>
	 </tr>
	<% }); %>
	</tbody>
</table>