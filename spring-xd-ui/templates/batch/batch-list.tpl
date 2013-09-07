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
                <td>Last execution status</td>
                <td>Last execution parameters</td>
                <td>Last execution start time</td>
                <td>Last execution duration</td>
                <td>Actions</td>
            </tr>
        </thead>
        <tbody>
            <% jobs.forEach(function(job) { %>
            <tr id="<%= job.name %>">
                <td>
                  <!-- don't use data-toggle and data-target for collapsing rows in bootstrap 2.3.2
                       see stackoverflow.com/questions/18495653/how-do-i-collapse-a-table-row-in-bootstrap/18496059#18496059
                       data-toggle="collapse" data-target="#<%= job.detailsRow %>" 
                       use jquery's show and hide instead    
                   -->
                  <button class="btn detailAction" type="button" job-name="<%= job.name %>">
                    <strong><a><%= job.name %></a>
                  </strong></button>
                </td>
                <% if (job.hasExecutions) { %>
                    <td><%= job.executionCount %></td>
                    <td> <div class="<%= job.alertClass %>"><%= job.exitStatus %></div></td>
                    <td><%= job.jobParameters %></td>
                    <td><%= job.startTime %></td>
                    <td><%= job.duration %></td>
                <% } else {%>
                    <td colspan="5"><em>No executions</em></td>
                <% } %>
                <td>
                  <button class="btn btn-mini launchAction <%= job.launchable %>" 
                    job-name="<%= job.name %>" type="button">Launch</button>
                  <button class="btn btn-mini launchWithParametersAction <%= job.launchable %>" 
                    job-name="<%= job.name %>" type="button">Launch with parameters...</button>
                </td>
            </tr>
            <tr id="<%= job.detailsRow %>" class="jobDetailsRow"><td colspan="4"><div id="<%= job.details %>"></div></td></tr>
            <% }); %>
        </tbody>
    </table>
</div>