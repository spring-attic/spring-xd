				<select name="type" class="form-control">
				<% datatypes.each(function(datatype) { %>
					<% if (datatype.get('selected')) { %>
						<option value="<%= datatype.get('id') %>" selected><%= datatype.get('name') %></option>
					<% } else {%>
						<option value="<%= datatype.get('id') %>"><%= datatype.get('name') %></option>
					<% }}); %>
				</select>