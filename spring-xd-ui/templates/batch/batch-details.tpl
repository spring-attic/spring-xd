<div class="row">
    <div class="span5 offset1">
        <h5>Instances of job <%= name %></h5>
        <table class="table table-bordered table-striped info-table table-hover" id="batch-instances-<%= name %>">
            <thead>
        		<tr>
        			<td>ID</td>
        			<td>Execution count</td>
        			<td>Last Execution Status</td>
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
        	       case 'COMPLETED':
        	           alertClass = 'alert alert-success';
        	           break;
        	   }
        	%>
            <tr id="<%= name %>-instance-<%= jobModel.id %>" class="jobInstance" instanceId="<%= jobModel.id %>">
        	    <td><%= jobModel.id %></td>
        	    <td><%= jobInstance.executionCount %></td>
        	    <td><div class="<%= alertClass %>">
        	       <%= jobInstance.lastJobExecutionStatus %>
        	    </div></td>
        	    <td><%= JSON.stringify(jobInstance.jobParameters) %></td>
        	 </tr>
        	<% }); %>
        	</tbody>
        </table>
    </div>
    <div class="span4">
        <h5>Executions</h5>
        <div id="<%= name %>-executions" class=>
        Click instance to see execution details.
        </div>
    </div>
</div>	
