			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
				<h4 class="modal-title" id="job-launch-label">Job Parameters for Job <span id="job-launch-jobname"><%=jobname %></span></h4>
			</div>
			<div class="modal-body" id="job-params-form">
				<form class="form-horizontal" role="form">
					<fieldset id="jobParameters"></fieldset>
				</form>
			</div>
			<div class="modal-footer">
				<div class="pull-left">
					<button type="button" class="btn btn-default add-job-param">
					<span class="glyphicon glyphicon-plus"></span> Param</button>
				</div>
				<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
				<button type="button" class="btn btn-primary launch-job">Launch Job <span class="glyphicon glyphicon-play"></span></button>
			</div>