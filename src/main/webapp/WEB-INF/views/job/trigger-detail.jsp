<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ include file="/WEB-INF/views/header.inc" %>
<%@ include file="/WEB-INF/views/job/submenu.inc" %>
<div class="content">

<h1>Trigger Detail </h1>

<div class="center">
<a href="${ mainPath }/job/unschedule?triggerName=${ data.firstTrigger.name }&triggerGroup=${ data.firstTrigger.group }"> UNSCHEDULE THIS TRIGGER JOB</a>
</div>

<div class="center">
The job class for this trigger is : ${ data.jobDetail.jobClass.name }. 
View full <a href="${ mainPath }/job/job-detail?jobName=${ data.jobDetail.name }&jobGroup=${ data.jobDetail.group }">JOB DETAIL</a> here.
</div>
		
<c:set var="trigger" value="${ data.firstTrigger }" scope="request"/>	
<%@ include file="/WEB-INF/views/job/trigger-detail.inc" %>

<h2>Trigger's Next ${ data.fireTimesCount } FireTimes</h2>
<table class="outlined datalist">
	<c:forEach items="${ data.nextFireTimes }" var="time" varStatus="status">
	<tr>
		<td>${ status.index + 1 }</td>
		<td>${ time }</td>
	</tr>
	</c:forEach>
</table>

</div><!-- div.content -->
<%@ include file="/WEB-INF/views/footer.inc" %>