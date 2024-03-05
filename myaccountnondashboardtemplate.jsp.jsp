<%@ page trimDirectiveWhitespaces="true"%>
<%@ taglib prefix="template" tagdir="/WEB-INF/tags/desktop/template"%>
<%@ taglib prefix="cms" uri="http://hybris.com/tld/cmstags"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="common" tagdir="/WEB-INF/tags/desktop/common"%>
<%@ taglib prefix="breadcrumb"
	tagdir="/WEB-INF/tags/desktop/nav/breadcrumb"%>
<template:page pageTitle="${pageTitle}">
  <jsp:body>
			<div id="globalMessages">
					<common:globalMessages />
				</div>
				<div class="my_acc_dtls">
                		<div class="container">
                		<div id="breadcrumb" class="breadcrumb">
                			<breadcrumb:breadcrumb breadcrumbs="${breadcrumbs}"/>
                		</div>
                	</div>

                	<cms:pageSlot position="GainMomentSlotName" var="gainmoment">
                    							<cms:component component="${gainmoment}" element="div" />
                    						</cms:pageSlot>
                    <cms:pageSlot position="MilkSolidsSlotName" var="milksolids">
                                        							<cms:component component="${milksolids}" element="div" />
                                        						</cms:pageSlot>``

	</jsp:body>
</template:page>

