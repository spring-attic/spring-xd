    <!-- requires: name, status, definition, isExpanded -->
    	<% var details = name + '_details', detailsRow = name + '_detailsRow', btnClass = status === 'deployed' ? 'btn-success' : 'btn-warning',
    	   analyticsName = name + '_analytics', gaugeName = name + '_gauge',
    	   expandedClass = (isExpanded ? 'in' : 'out'); %>
        <tr id="<%= name %>">
    		<td>
    		  <button class="btn <%= btnClass %> detailAction" type="button" data-toggle="collapse" data-target="#<%= details %>">
    		    <strong><a><%= name %></a>
    		  </strong></button>
    		</td>
    		<td><%= definition %>
    		</td>
    		<td><button class="btn btn-mini deleteAction" type="button"><img height="16" width="16" title="Delete" src="images/close.png"/></button></td>
    	</tr>
    	<tr id="<%= detailsRow %>"><td colspan="3"><div id="<%= details %>" class="collapse <%= expandedClass %>">
    	   <div class="well artifact_details">
    	       The Analytics for artifact <strong><%= name %></strong> goes here.
    	       We can also add buttons, commands, and other things to send messages
    	       to the artifacts.
    	       <div class="guage_analytics" id="<%= gaugeName %>"></div>
    	       <br />
    	       <div class="artifact_analytics" id="<%= analyticsName %>"></div>
    	   </div></div></td></tr>
