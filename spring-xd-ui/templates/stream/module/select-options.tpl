<span class="add-on"><strong><%= module.name %></strong></span>
    						<select id="<%= module.selector %>">	
								<% if (!_.isEmpty(module.options)) { %>
								<option selected="selected"></option>
        						<% module.options.forEach(function(option) { %>
            					<option><%= option %></option>
       							<% }); %>
								<% } %>
    						</select>