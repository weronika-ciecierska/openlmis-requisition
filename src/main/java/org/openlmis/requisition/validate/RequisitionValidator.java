package org.openlmis.requisition.validate;

import static org.springframework.util.CollectionUtils.isEmpty;

import org.openlmis.requisition.domain.Requisition;
import org.openlmis.requisition.domain.RequisitionLineItem;
import org.openlmis.requisition.domain.RequisitionStatus;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.exception.RequisitionTemplateColumnException;
import org.openlmis.requisition.repository.RequisitionTemplateRepository;
import org.openlmis.settings.service.ConfigurationSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
public class RequisitionValidator extends AbstractRequisitionValidator {

  static final String VALUE_MUST_BE_ENTERED_NOTIFICATION =
      " must be entered prior to submission of a requisition.";
  static final String VALUE_MUST_BE_NON_NEGATIVE_NOTIFICATION =
      " must be a non-negative value.";
  static final String TEMPLATE_COLUMN_IS_HIDDEN =
      " is hidden in template and should not contain a value.";

  @Autowired
  private RequisitionTemplateRepository requisitionTemplateRepository;

  @Autowired
  private ConfigurationSettingService configurationSettingService;

  @Override
  public void validate(Object target, Errors errors) {
    Requisition requisition = (Requisition) target;

    if (isEmpty(requisition.getRequisitionLineItems())) {
      errors.rejectValue(REQUISITION_LINE_ITEMS,
          "A requisitionLineItems" + VALUE_MUST_BE_ENTERED_NOTIFICATION);
    } else {
      RequisitionTemplate template = requisitionTemplateRepository.getTemplateForProgram(
          requisition.getProgramId()
      );

      requisition.getRequisitionLineItems()
          .forEach(i -> validateRequisitionLineItem(errors, template, requisition, i));
    }
  }

  private void validateRequisitionLineItem(Errors errors, RequisitionTemplate template,
                                           Requisition requisition, RequisitionLineItem item) {
    rejectIfNull(errors, template, item.getRequestedQuantity(),
        RequisitionLineItem.REQUESTED_QUANTITY);
    rejectIfLessThanZero(errors, template, item.getRequestedQuantity(),
        RequisitionLineItem.REQUESTED_QUANTITY);

    rejectIfNull(errors, template, item.getBeginningBalance(),
        RequisitionLineItem.BEGINNING_BALANCE);
    rejectIfLessThanZero(errors, template, item.getBeginningBalance(),
        RequisitionLineItem.BEGINNING_BALANCE);

    rejectIfNull(errors, template, item.getTotalReceivedQuantity(),
        RequisitionLineItem.TOTAL_RECEIVED_QUANTITY);
    rejectIfLessThanZero(errors, template, item.getTotalReceivedQuantity(),
        RequisitionLineItem.TOTAL_RECEIVED_QUANTITY);

    rejectIfNull(errors, template, item.getStockOnHand(),
        RequisitionLineItem.STOCK_ON_HAND);
    rejectIfLessThanZero(errors, template, item.getStockOnHand(),
        RequisitionLineItem.STOCK_ON_HAND);

    rejectIfNull(errors, template, item.getTotalConsumedQuantity(),
        RequisitionLineItem.TOTAL_CONSUMED_QUANTITY);
    rejectIfLessThanZero(errors, template, item.getTotalConsumedQuantity(),
        RequisitionLineItem.TOTAL_CONSUMED_QUANTITY);

    rejectIfNull(errors, template, item.getTotalLossesAndAdjustments(),
        RequisitionLineItem.TOTAL_LOSSES_AND_ADJUSTMENTS);

    validateApprovedQuantity(errors, template, requisition, item);

    checkTemplate(errors, template, item.getRequestedQuantityExplanation(),
        RequisitionLineItem.REQUESTED_QUANTITY_EXPLANATION);
  }

  private void rejectIfLessThanZero(Errors errors, RequisitionTemplate template,
                                    Integer value, String field) {
    boolean templateValid = checkTemplate(errors, template, value, field);

    if (templateValid && value != null && value < 0) {
      errors.rejectValue(REQUISITION_LINE_ITEMS, field + VALUE_MUST_BE_NON_NEGATIVE_NOTIFICATION);
    }
  }

  private void rejectIfNull(Errors errors, RequisitionTemplate template,
                            Object value, String field) {
    boolean templateValid = checkTemplate(errors, template, value, field);

    if (templateValid && value == null) {
      errors.rejectValue(REQUISITION_LINE_ITEMS, field + VALUE_MUST_BE_ENTERED_NOTIFICATION);
    }
  }

  private void validateApprovedQuantity(Errors errors, RequisitionTemplate template,
                                        Requisition requisition, RequisitionLineItem item) {
    if (requisition.getStatus() == RequisitionStatus.AUTHORIZED
        || (configurationSettingService.getBoolValue("skipAuthorization")
        && requisition.getStatus() == RequisitionStatus.SUBMITTED)) {

      rejectIfNull(errors, template, item.getApprovedQuantity(),
          RequisitionLineItem.APPROVED_QUANTITY);
      rejectIfLessThanZero(errors, template, item.getApprovedQuantity(),
          RequisitionLineItem.APPROVED_QUANTITY);
    }
  }

  private boolean checkTemplate(Errors errors, RequisitionTemplate template,
                                Object value, String field) {
    try {
      return checkIfDisplayed(errors, template, value, field);
    } catch (RequisitionTemplateColumnException ex) {
      errors.rejectValue(REQUISITION_LINE_ITEMS, ex.getMessage());
    }

    return false;
  }

  private boolean checkIfDisplayed(Errors errors, RequisitionTemplate template, Object value,
                                   String field) throws RequisitionTemplateColumnException {
    if (!template.isColumnDisplayed(field)) {
      if (value != null) {
        errors.rejectValue(REQUISITION_LINE_ITEMS, field + TEMPLATE_COLUMN_IS_HIDDEN);
      }

      return false;
    }

    return true;
  }
}
