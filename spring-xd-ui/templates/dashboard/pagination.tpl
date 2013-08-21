<div class="pagination pagination-centered pagination-small">
									<ul id="pagelinks">
									<% for (var i=0;i<pages;i++) { %>
										<li pageNumber="<%= i %>" class="<%= i === current_page ? 'pagination active' : 'pagination' %>">
										  <a><%= i+1 %></a></li>
									<% }; %>
									</ul>
							</div>
