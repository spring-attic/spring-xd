<% if ( status.indexOf("Success") !== -1) {%>
<% var alert_class = "alert-success"; %>
<% } %>
<% if ( status.indexOf("Error") !== -1) {%>
<% var alert_class = "alert-error"; %>
<% } %>
<div class="alert <%= alert_class %>">
  <button type="button" class="close" data-dismiss="alert">&times;</button>
  <strong><%= status %> </strong><%= message %> 
</div>
