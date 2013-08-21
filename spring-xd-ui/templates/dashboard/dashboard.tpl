<div id="dashboard-sidebar" class="span9">
	<div id="sidebar-tab">
					<ul class="nav nav-tabs">
   					    <% kindNames.forEach(function(kindName) { %>
					      <li <%= kindName.kind === 'streams' ? 'class="active"' : '' %>>
					         <a href="#xd-<%= kindName.kind %>-list" data-toggle="tab"><%= kindName.name %></a></li>
					    <% });%>
				      <li>
				         <a href="#xd-batch-list" data-toggle="tab">Batch</a></li>
					</ul>
				</div>
				<div id="sidebar-content" class="tab-content">
   					<% kindNames.forEach(function(kindName) { %>
    				    <div class="tab-pane <%= kindName.kind === 'streams' ? 'active' : '' %>" id="xd-<%= kindName.kind %>-list"> 
    				    	<div id="<%= kindName.kind %>-table"></div>
    				    	<div id="<%= kindName.kind %>-pagination"></div>
    				    </div>
				    <% });%>
				    <!-- Do batch separately for now -->
				    <div class="tab-pane" id="xd-batch-list"> 
				    	<div id="batch-table"></div>
				    </div>
				</div>
            </div>
        </div>
