<#include "component://setup/webapp/setup/common/common.ftl">

<#-- FIXME?: either move or find better solution, but @cell works badly here, need float or inline-block -->
<style type="text/css">
  .setupAccounting-selectGL-select, .setupAccounting-selectGL-submit-buttons {
    display:inline-block;
  }
  .setupAccounting-selectGL-select {
    margin-right:0.5em;
  }
  .setupAccounting-selectGL-select, .setupAccounting-selectGL-submit-buttons {
    vertical-align:top; <#-- firefox align issue hack -->
  }
</style>

<@script>
    jQuery(document).ready(function() {
        var submitSelectAccountingForm = function(cntd) {
            var val = jQuery('#setupAccounting-selectGL-select').val();            
            if (val) {
                if (cntd) {
                    jQuery('#setupAccounting-selectContinueGL-form input[name=topGlAccountId]').val(val);
                    jQuery('#setupAccounting-selectContinueGL-form').submit();
                } else {
                    jQuery('#setupAccounting-selectGL-form').submit();
                } 
            } else {
                jQuery('#setupAccounting-newGL-form').submit();
            }
        };

        jQuery('#setupAccounting-selectGL-submit').click(function() {
            submitSelectAccountingForm(false);
        });
        jQuery('#setupAccounting-selectGL-submit-continue').click(function() {
            submitSelectAccountingForm(true);
        });
    });
</@script>
<@alert type="warning">WARNING: WORK-IN-PROGRESS</@alert>
  <@form method="get" action=makeOfbizUrl("setupAccounting") id="setupAccounting-selectGL-form">
    <#-- TODO: REVIEW: may make a difference later -->
    <@defaultWizardFormFields exclude=["topGlAccountId"]/>
    <#--<@field type="hidden" name="setupContinue" value="N"/> not needed yet-->
    
    <@field type="general" label=uiLabelMap.SetupAccountingSelectModuleForSetup>
       <@field type="select" name="topGlAccountId" id="setupAccounting-selectGL-select" class="+setupAccounting-selectGL-select" inline=true style="display:inline-block;">
            <option value="">[${uiLabelMap.SetupAccountingCreateNewModule}]</option>
            <option value="" disabled="disabled"></option>
            <#if accountingGLs?has_content>
              <#list accountingGLs as accountingGL>
              	<#assign selected = (rawString(accountingGL.glAccountId) == rawString(topGlAccountId!))>
                <option value="${accountingGL.glAccountId!}"<#if selected> selected="selected"</#if>>${accountingGL.accountCode!} [${accountingGL.glAccountId!}]</option>
              </#list>
            </#if>
        </@field>
        <@menu type="button" id="setupAccounting-selectGL-submit-buttons" class="+setupAccounting-selectGL-submit-buttons">
          <@menuitem type="link" id="setupAccounting-selectGL-submit" href="javascript:void(0);" text=uiLabelMap.CommonSelect class="+${styles.action_run_session!} ${styles.action_update!}"/>
          <@menuitem type="link" id="setupAccounting-selectGL-submit-continue" href="javascript:void(0);" text=uiLabelMap.SetupSelectAndContinue class="+${styles.action_run_session!} ${styles.action_continue!}"/>
        </@menu>
    </@field>        
  </@form>
  
  <@form method="get" action=makeOfbizUrl("setupWizard") id="setupAccounting-selectContinueGL-form">
    <#-- TODO: REVIEW: may make a difference later -->
    <@defaultWizardFormFields exclude=["topGlAccountId"]/>
    <#--<@field type="hidden" name="setupContinue" value="Y"/> not needed yet-->
  
    <@field type="hidden" name="topGlAccountId" value=""/>
  </@form>
  
  <@form method="get" action=makeOfbizUrl("setupAccounting") id="setupAccounting-newGL-form">
    <#-- TODO: REVIEW: may make a difference later -->
    <@defaultWizardFormFields exclude=["topGlAccountId"]/>    
    <@field type="hidden" name="newGlAccount" value="Y"/>
  </@form>
  
  