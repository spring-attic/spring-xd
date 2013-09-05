<div>
    <div class="row">
        <div class="span1 offset10">
            <form class="form-search">
                <input type="text" id="job_filter" class="input-small search-query" placeholder="Filter"/>
            </form>
        </div>
    </div>
    <table class="table table-bordered table-striped info-table" id="batch">
    	<thead>
    		<tr>
    			<td>Job name</td>
    			<td>Execution count</td>
    			<td>Actions</td>
    		</tr>
    	</thead>
    	<tbody>
        	<% jobs.forEach(function(job) {
        	   var name = job.attributes.name, details = name + '_details', detailsRow = name + '_detailsRow'; %>
            <tr id="<%= name %>">
        		<td>
        		  <!-- don't use data-toggle and data-target for collapsing rows in bootstrap 2.3.2
        		       see stackoverflow.com/questions/18495653/how-do-i-collapse-a-table-row-in-bootstrap/18496059#18496059
        		       data-toggle="collapse" data-target="#<%= detailsRow %>" 
        		       use jquery's show and hide instead    
        		   -->
        		  <button class="btn detailAction" type="button" job-name="<%= name %>">
        		    <strong><a><%= name %></a>
        		  </strong></button>
        		</td>
        		<td><%= job.attributes.executionCount %></td>
        		<td>
        		  <button class="btn btn-mini launchAction <%=job.attributes.launchable ? '' : 'disabled'%>" job-name="<%= name %>" type="button">Launch</button>
        	    </td>
        	</tr>
        	<tr id="<%= detailsRow %>" class="jobDetailsRow"><td colspan="4"><div id="<%= details %>"></div></td></tr>
            <% }); %>
    	</tbody>
    </table>
</div>