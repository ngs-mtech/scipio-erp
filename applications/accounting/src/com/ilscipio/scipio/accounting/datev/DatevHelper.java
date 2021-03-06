package com.ilscipio.scipio.accounting.datev;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityJoinOperator;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtil;

import com.ilscipio.scipio.accounting.datev.DatevNotificationMessage.NotificationMessageType;

import javolution.util.FastMap;

public class DatevHelper {
    private static final String module = DatevHelper.class.getName();

    private final List<GenericValue> datevTransactionEntryDefinitions;
    private final List<GenericValue> datevMetadataTransactionEntryDefinitions;    
    private final List<String> datevTransactionFieldNames;
    private final String orgPartyId;
    
    private Map<String, Object> datevMetadataValues = FastMap.newInstance();

    private List<DatevNotificationMessage> notificationMessages;

    static enum DatevFieldType {
        TEXT(String.class), NUMBER(Number.class), AMOUNT(BigDecimal.class), DATE(DateTime.class), BOOLEAN(Boolean.class);

        private final Class<?> fieldTypeClass;

        DatevFieldType(Class<?> clazz) {
            this.fieldTypeClass = clazz;
        }

        public Class<?> getFieldTypeClass() {
            return fieldTypeClass;
        }

        @Override
        public String toString() {
            return "FieldTypeName [" + this.name() + "]: " + fieldTypeClass.getName();
        }

    }

    // private final ResultSet datevTransactionEntryDefinitionsResultSet;
    // private final ResultSet
    // datevMetadataTransactionEntryDefinitionsResultSet;

    // private Map<String, Boolean> metaHeaderFieldsFound =
    // FastMap.newInstance();
    // private Map<String, Boolean> headerFieldsFound = FastMap.newInstance();

    public DatevHelper(Delegator delegator, String orgPartyId, List<DatevNotificationMessage> notificationMessages) throws DatevException {
        try {
            this.datevTransactionEntryDefinitions = EntityQuery.use(delegator).from("DatevTransactionEntryDefinition")
                    .where(EntityCondition.makeConditionWhere("METADATA IS NULL OR METADATA = 'N'")).queryList();

            this.datevTransactionFieldNames = EntityUtil.getFieldListFromEntityList(datevTransactionEntryDefinitions, "fieldName", true);
            this.datevMetadataTransactionEntryDefinitions = EntityQuery.use(delegator).from("DatevTransactionEntryDefinition")
                    .where(EntityCondition.makeCondition("metadata", EntityOperator.EQUALS, "Y")).queryList();
            this.notificationMessages = notificationMessages;
            this.orgPartyId = orgPartyId;
        } catch (GenericEntityException e) {
            throw new DatevException(new DatevNotificationMessage(NotificationMessageType.FATAL, "Internal error. Cannot initialize DATEV helper."));
        }
    }

    public boolean isMetaHeader(Iterator<String> metaHeaderIter) throws DatevException {
        boolean hasMetaHeader = true;
        for (int i = 0; metaHeaderIter.hasNext(); i++) {
            GenericValue fieldDefinition = null;
            try {
                fieldDefinition = datevMetadataTransactionEntryDefinitions.get(i);
            } catch (IndexOutOfBoundsException e) {
                notificationMessages.add(new DatevNotificationMessage(NotificationMessageType.WARNING,
                        "Metadata header size doesn't match the expected size [" + datevMetadataTransactionEntryDefinitions.size() + "]"));
                hasMetaHeader = false;
                break;
            }
            if (UtilValidate.isNotEmpty(fieldDefinition)) {
                String metaHeaderValue = metaHeaderIter.next();
                boolean isMetadataFieldValid = validateField(fieldDefinition, metaHeaderValue);
                if (!isMetadataFieldValid) {
                    notificationMessages.add(new DatevNotificationMessage(NotificationMessageType.WARNING,
                            "Metadata header field [" + fieldDefinition.getString("fieldName") + "] is not valid for value <" + metaHeaderValue + ">"));                    
                }
                datevMetadataValues.put(fieldDefinition.getString("fieldName"), UtilMisc.toMap(metaHeaderValue, isMetadataFieldValid));
            }
            if (i > datevMetadataTransactionEntryDefinitions.size() - 1) {
                notificationMessages.add(new DatevNotificationMessage(NotificationMessageType.WARNING,
                        "Metadata header size doesn't match the expected size [" + datevMetadataTransactionEntryDefinitions.size() + "]"));
                hasMetaHeader = false;
            }
        }

        return hasMetaHeader;
    }

    public String[] getDatevTransactionFieldNames() {
        String[] fieldNames = new String[datevTransactionFieldNames.size()];
        return datevTransactionFieldNames.toArray(fieldNames);
    }

    public void processRecord(Map<String, String> recordMap) throws DatevException {

    }

    public boolean validateField(int position, String value) throws DatevException {
        return validateField(EntityUtil.getFirst(
                EntityUtil.filterByCondition(datevTransactionEntryDefinitions, EntityCondition.makeCondition("sequenceNum", EntityJoinOperator.EQUALS, position))), value);
    }

    public boolean validateField(String fieldName, String value) throws DatevException {
        return validateField(EntityUtil
                .getFirst(EntityUtil.filterByCondition(datevTransactionEntryDefinitions, EntityCondition.makeCondition("fieldName", EntityJoinOperator.EQUALS, fieldName))), value);
    }

    public boolean validateField(GenericValue fieldDefinition, String value) {
        String fieldName = fieldDefinition.getString("fieldName");
        String type = fieldDefinition.getString("typeEnumId");
        try {
            long length = -1;
            if (UtilValidate.isNotEmpty(fieldDefinition.get("length"))) {
                length = fieldDefinition.getLong("length");
            }
            long scale = 0;
            if (UtilValidate.isNotEmpty(fieldDefinition.get("length"))) {
                scale = fieldDefinition.getLong("scale");
            }
            long maxLength = -1;
            if (UtilValidate.isNotEmpty(fieldDefinition.get("maxLength"))) {
                maxLength = fieldDefinition.getLong("maxLength");
            }
            String format = null;
            if (UtilValidate.isNotEmpty(fieldDefinition.get("format"))) {
                format = fieldDefinition.getString("format");
            }
            boolean required = false;
            if (UtilValidate.isNotEmpty(fieldDefinition.get("required"))) {
                required = fieldDefinition.getBoolean("required");
            }
            boolean metadata = false;
            if (UtilValidate.isNotEmpty(fieldDefinition.get("metadata"))) {
                metadata = fieldDefinition.getBoolean("metadata");
            }

            GenericValue fieldTypeEnum = fieldDefinition.getRelatedOne("DatevFieldTypeEnumeration", true);
            DatevFieldType datevFieldType = DatevFieldType.valueOf(fieldTypeEnum.getString("enumCode"));

            if (Debug.isOn(Debug.VERBOSE)) {
                Debug.log("Validating datev field [" + fieldName + "]:" + "\r\n\t type: " + fieldTypeEnum.getString("enumCode") + "\r\n\t length: " + length + "\r\n\t scale: "
                        + scale + "\r\n\t maxLength: " + maxLength + "\r\n\t format: " + format + "\r\n\t required: " + required + "\r\n\t metadata" + metadata);
            }

            if (UtilValidate.isEmpty(value)) {
                if (required) {
                    notificationMessages.add(new DatevNotificationMessage(NotificationMessageType.RECORD_ERROR, "Required field [" + fieldName + "] has no value"));
                    return false;
                } else {
                    return true;
                }
            }

            if (value.length() > maxLength) {
                notificationMessages.add(new DatevNotificationMessage(NotificationMessageType.RECORD_ERROR,
                        "Field [" + fieldName + "] length <" + value.length() + "> is greater than the max value allowed for it <" + maxLength + ">"));
                return false;
            }

            Object validatedValue = null;
            switch (datevFieldType) {
            case TEXT:
                validatedValue = value;
                break;
            case BOOLEAN:
                validatedValue = Boolean.valueOf(value);
                break;
            case DATE:
                DateTimeFormatter dtf = null;
                if (UtilValidate.isNotEmpty(format)) {
                    dtf = DateTimeFormat.forPattern(format);
                }
                validatedValue = DateTime.parse(value, dtf);
                break;
            case NUMBER:
                validatedValue = Integer.valueOf(value);
                break;
            case AMOUNT:
                validatedValue = BigDecimal.valueOf(Double.valueOf(value));
                if (UtilValidate.isNotEmpty(validatedValue) && UtilValidate.isNotEmpty(scale)) {
                    validatedValue = ((BigDecimal) validatedValue).setScale(Math.toIntExact(scale));
                }
                break;
            default:
                notificationMessages.add(new DatevNotificationMessage(NotificationMessageType.WARNING, "Type [" + type + "] is not supported for value: " + value));
                return false;
            }
        } catch (Exception e) {
            notificationMessages.add(new DatevNotificationMessage(NotificationMessageType.WARNING,
                    "Can't convert [" + value + "] to type " + type + " for field <" + fieldDefinition.getString("fieldName") + ">"));
            return false;
        }

        return true;
    }

    public List<DatevNotificationMessage> getNotificationMessages() {
        return notificationMessages;
    }

}
