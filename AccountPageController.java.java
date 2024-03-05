/*
 * [y] hybris Platform
 *
 * Copyright (c) 2000-2013 hybris AG
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of hybris
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with hybris.
 *
 *
 */
package com.glanbia.agri.storefront.controllers.pages;

import com.glanbia.agri.core.services.emarsys.EmarsysContactDataService;
import com.glanbia.agri.core.data.cpi.gap.GapActionResult;
import com.glanbia.agri.core.data.cpi.gap.GapUserAction;
import com.glanbia.agri.core.util.MilkCalanderUtil;
import com.glanbia.agri.facades.survey.GlanbiaCustomerSurveyFacade;
import com.glanbia.agri.facades.survey.data.SurveyData;
import com.glanbia.agri.facades.vetcustomer.data.VetCustomerData;
import com.glanbia.agri.storefront.forms.AddressForm;
import com.glanbia.agri.storefront.forms.UpdateEmailForm;
import com.glanbia.agri.storefront.forms.UpdatePasswordForm;
import com.glanbia.agri.storefront.forms.UpdateProfileForm;
import com.glanbia.agri.storefront.forms.OrderApprovalDecisionForm;
import com.glanbia.agri.storefront.forms.QuoteOrderForm;
import com.glanbia.agri.storefront.forms.BulkOrderForm;
import com.glanbia.agri.storefront.forms.BulkOrderConfirmationForm;
import com.glanbia.agri.storefront.forms.SurveyForm;
import com.glanbia.agri.storefront.forms.ReorderForm;
import de.hybris.platform.acceleratorfacades.device.data.DeviceData;
import de.hybris.platform.acceleratorfacades.device.impl.DefaultDeviceDetectionFacade;
import de.hybris.platform.acceleratorstorefrontcommons.forms.ConsentForm;
import de.hybris.platform.b2bacceleratorfacades.order.B2BOrderFacade;
import de.hybris.platform.b2bacceleratorfacades.order.data.B2BOrderApprovalData;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.pages.ContentPageModel;
import de.hybris.platform.commercefacades.consent.CustomerConsentDataStrategy;
import de.hybris.platform.commercefacades.customer.CustomerFacade;
import de.hybris.platform.commercefacades.i18n.I18NFacade;
import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.*;
import de.hybris.platform.commercefacades.product.data.CategoryData;
import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.commercefacades.product.data.PriceDataType;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commercefacades.search.ProductSearchFacade;
import de.hybris.platform.commercefacades.search.data.SearchQueryData;
import de.hybris.platform.commercefacades.search.data.SearchStateData;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.commercefacades.user.data.CustomerData;
import de.hybris.platform.commercefacades.user.data.RegionData;
import de.hybris.platform.commercefacades.user.data.TitleData;
import de.hybris.platform.commercefacades.user.exceptions.PasswordMismatchException;
import de.hybris.platform.commerceservices.consent.exceptions.CommerceConsentGivenException;
import de.hybris.platform.commerceservices.consent.exceptions.CommerceConsentWithdrawnException;
import de.hybris.platform.commerceservices.customer.DuplicateUidException;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.order.CommerceCartModificationStatus;
import de.hybris.platform.commerceservices.search.facetdata.ProductCategorySearchPageData;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commerceservices.search.pagedata.SearchPageData;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.enumeration.EnumerationService;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.servicelayer.exceptions.AmbiguousIdentifierException;
import de.hybris.platform.servicelayer.exceptions.BusinessException;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.util.Config;
import de.hybris.platform.workflow.enums.WorkflowActionType;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.glanbia.agri.core.constants.AgriCoreConstants;
import com.glanbia.agri.core.constants.WebserviceConstants;
import com.glanbia.agri.core.data.EtOutput;
import com.glanbia.agri.core.data.MilkResults;
import com.glanbia.agri.core.data.ServiceRespMapper;
import com.glanbia.agri.core.data.ZArchive_Library;
import com.glanbia.agri.core.data.cpi.CPIServiceRespMapper;
import com.glanbia.agri.core.data.cpi.EtMonthsVolComp;
import com.glanbia.agri.core.data.cpi.EtYearsVolComp;
import com.glanbia.agri.core.model.VetCustomerModel;
import com.glanbia.agri.core.services.customer.GlanbiaCustomerEmailResolutionService;
import com.glanbia.agri.core.util.DateUtil;
import com.glanbia.agri.facades.bulkOrder.BulkOrderFacade;
import com.glanbia.agri.facades.bulkOrder.bulkOrder.data.BulkOrderData;
import com.glanbia.agri.facades.cache.SapPriceCacheFacade;
import com.glanbia.agri.facades.user.VetUserFacade;
import com.glanbia.agri.facades.user.data.VetAddressData;
import com.glanbia.agri.facades.vetProduct.VetProductFacade;
import com.glanbia.agri.facades.webservice.impl.DefaultMyAccountFacade;
import com.glanbia.agri.facades.webservice.impl.MyAccountDashboardFacade;
import com.glanbia.agri.storefront.annotations.RequireHardLogIn;
import com.glanbia.agri.storefront.breadcrumb.Breadcrumb;
import com.glanbia.agri.storefront.breadcrumb.ResourceBreadcrumbBuilder;
import com.glanbia.agri.storefront.breadcrumb.impl.ContentPageBreadcrumbBuilder;
import com.glanbia.agri.storefront.controllers.ControllerConstants;
import com.glanbia.agri.storefront.controllers.ThirdPartyConstants;
import com.glanbia.agri.storefront.controllers.util.GlobalMessages;
import com.glanbia.agri.storefront.forms.validation.AddressValidator;
import com.glanbia.agri.storefront.util.XSSFilterUtil;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.gson.Gson;



/**
 * Controller for home page.
 */
@Controller
@Scope("tenant")
@RequestMapping("/my-account")
@SuppressWarnings("squid:S1258")
public class AccountPageController extends AbstractSearchPageController
{
	private static final String WEBSERVICE_NOT_AVAILABLE_ERROR_MESSAGE = "webservice.not.available.error.message";
	// Internal Redirects
	private static final String REDIRECT_MY_ACCOUNT = REDIRECT_PREFIX + "/my-account";
	private static final String ADDRESS_BOOK = "/my-account/address-book";
	private static final String REDIRECT_TO_ADDRESS_BOOK_PAGE = REDIRECT_PREFIX + ADDRESS_BOOK;
	private static final String REDIRECT_TO_PAYMENT_INFO_PAGE = REDIRECT_PREFIX + "/my-account/payment-details";
	private static final String REDIRECT_TO_PROFILE_PAGE = REDIRECT_PREFIX + "/my-account/profile";

	/**
	 * We use this suffix pattern because of an issue with Spring 3.1 where a Uri value is incorrectly extracted if it
	 * contains on or more '.' characters. Please see https://jira.springsource.org/browse/SPR-6164 for a discussion on
	 * the issue and future resolution.
	 */
	private static final String ORDER_CODE_PATH_VARIABLE_PATTERN = "{orderCode:.*}";
	private static final String ADDRESS_CODE_PATH_VARIABLE_PATTERN = "{addressCode:.*}";
	private static final String WORKFLOW_ACTION_CODE_PATH_VARIABLE_PATTERN = "{workflowActionCode:.*}";

	// CMS Pages
	private static final String ACCOUNT_CMS_PAGE = "account";
	private static final String PROFILE_CMS_PAGE = "profile";
	private static final String ADDRESS_BOOK_CMS_PAGE = "address-book";
	private static final String ADD_EDIT_ADDRESS_CMS_PAGE = "add-edit-address";
	private static final String PAYMENT_DETAILS_CMS_PAGE = "payment-details";
	private static final String ORDER_HISTORY_CMS_PAGE = "orders";
	private static final String ORDER_DETAIL_CMS_PAGE = "order";
	private static final String ORDER_APPROVAL_DASHBOARD_CMS_PAGE = "order-approval-dashboard";

	private static final String MY_PURCHASED_PRODUCTS_CMS_PAGE = "my-purchased-products";

	private static final String MILK_SUPPLIER = "/my-account/milk-supplier";
	private static final String GRAIN_SUPPLIER = "/my-account/grain-supplier";
	private static final String TRAD_ACCOUNT = "/my-account/trading-account";
	private static final String NIR_USER = "/my-account/nir";
	private static final String GRAIN_TRAD = "/my-account/grain-trading";
	private static final String ONLINE_SHOPPER = "/my-account/online-shopper";
	private static final String EXPORT_FARM_PACKAGE = "/my-account/farmPackage";

	private static final String MILKGRAIN = "/my-account/milk-grain";
	private static final String MILKTRADINGGRAIN = "/my-account/milk-trading-grain";
	private static final String MILKTRADING = "/my-account/milk-trading";
	private static final String STAFF = "/my-account/staff";

	private static final String MILK_STATEMENTS_SUMMARY = "/my-account/milk-statement-summary";
	private static final String TRADING_STATEMENTS_SUMMARY = "/my-account/trading-account-summary";

	private static final String PAY_TRADING_ACCOUNT = "/my-account/pay-trading-account";

	private static final String GRAIN_STATEMENTS_SUMMARY = "/my-account/grain-statements";
	private static final String MILK_SUPPLY_FORECAST = "/my-account/milk-supply-forecast";
	private static final String MILK_SUPPLY_COLLECTION = "/my-account/supply-by-collection";
	private static final String MILK_SUPPLIER_DETAILS = "/milk-supplier-details";
	private static final String NOTIFICATION_CENTRE = "/my-account/notificationcentre";
	private static final String GUIDE = "/my-account/guide";
	private static final String MY_ACCOUNT_FAQ = "/my-account/myaccountfaq";

	private static final String YEARLY_SUPPLY_AND_QUOTA = "/my-account/yearly-supply-and-quota";

	private static final String POLICY_SUPPLY_AGREEMENT = "/my-account/my-supply-agreement";
	private static final String POLICY_LIQUID_PURCHASING = "/my-account/liquid-purchasing-policy";
	private static final String POLICY_MANUFACTURING = "/my-account/manufacturing-purchasing-policy";
	private static final String POLICY_FIXED_PRICE = "/my-account/fixed-pricing-scheme";
	private static final String POLICY_QUALITY_MANUAL = "/my-account/quality-manual";
	private static final String POLICY_SUSTAINABILITY_MANUAL = "/my-account/sustainability-manual";
	private static final String POLICY_NIR = "/my-account/nir-policy";
	private static final String POLICY_FARM_DEVELOPMENT = "/my-account/farm-development";
	private static final String FARM_MANAGEMENT_DOCS = "/my-account/farm-management-documents";
	private static final String PAPERLESS_OPTION_YES = "available";
	private static final String PAPERLESS_OPTION_NO = "unavailable";
	private static final String ZMKITSV001 = "ZMKITSV001";
	private static final String PEAKMILK = "/peak-milk";
	private static final String CURRENT_MONTH = "currentMonth";

	private static final String BREADCRUMBS = "breadcrumbs";
	private static final String STATUS = "status";
	private static final String PAPERLESS = "paperless";
	private static final String NO_PAPERLESS = "noPaperless";
	private static final String STATEMENT_TYPE = "statementType";
	private static final String CUSTOMER_TYPE = "customerType";
	private static final String CustomerType = "CustomerType";
	private static final String TEXT_ACCOUNT_PROFILE = "text.account.profile";
	private static final String TEXT_ACCOUNT_ADDRESSBOOK = "text.account.addressBook";
	private static final String TEXT_NOTIFICATION_CENTRE = "text.account.notification.centre";
	private static final String TEXT_MY_ACCOUNT_FAQ = "text.account.faq";

	private static final String SUSTAINABILITY_ACTION_PAYMENT = "/sustainability-action-payment";
	private static final String EDIT_PROFILE = REDIRECT_PREFIX + "/my-account/edit-profile";

	private static final String RECENT_TRANSACTION_SUMMARY = "/my-account/recent-transaction";

	private static final String PURCHASES_BY_CATEGORY = "/my-account/purchases-by-category";

	private static final String PURCHASES_BY_TYPE = "/my-account/purchases-by-type";

	private static final String GAIN_MOMENTUM_PROGRAMME = "gainmomentumtest1";

	private static final String NEW_BULK_FEED_ORDER = "bulkNewOrderPage";

	private static final String BULK_FEED_CATEGORY = "{bulkFeedCategoryCode:.*}";
	private static final String BULK_FEED_INCOTERM = "{incoterm:.*}";

	private static final String PREVIOUS_TEST_RESULTS_ROWS_COUNT = "previous.test.result.default.rows";

	private static final String REDIRECT_ORDER_LIST_URL = REDIRECT_PREFIX + "/my-account/orders/";

	//For Consent Management
	private static final String MARKETING_CONSENT = "registration.consent.id";
	private static final String TEXT_ACCOUNT_CONSENT_GIVEN = "text.account.consent.given";
	private static final String TEXT_ACCOUNT_CONSENT_WITHDRAWN = "text.account.consent.withdrawn";
	private static final String TEXT_ACCOUNT_CONSENT_NOT_FOUND = "text.account.consent.notFound";
	private static final String TEXT_ACCOUNT_CONSENT_TEMPLATE_NOT_FOUND = "text.account.consent.template.notFound";
	private static final String TEXT_ACCOUNT_CONSENT_ALREADY_GIVEN = "text.account.consent.already.given";
	private static final String TEXT_ACCOUNT_CONSENT_ALREADY_WITHDRAWN = "text.account.consent.already.withdrawn";

	private static final String SURVEY = "survey";

	private static final String ACCOUNT_SUSTAINABILITY_ACTION_PAYMENT_BREADCRUMBS = "account.sustainability.action.payment.breadcrumbs";

	@Resource(name = "userFacade")
	protected UserFacade userFacade;
	@Resource(name = "i18NFacade")
	protected I18NFacade i18NFacade;
	@Resource(name = "vetUserFacade")
	protected VetUserFacade vetUserFacade;
	@Resource(name = "b2bCustomerFacade")
	protected CustomerFacade customerFacade;
	@Resource(name = "sessionService")
	protected SessionService sessionService;
	@Resource(name = "gson")
	protected Gson gson;
	@Resource(name = "b2bOrderFacade")
	private B2BOrderFacade orderFacade;
	@Resource(name = "checkoutFacade")
	private CheckoutFacade checkoutFacade;
	@Resource(name = "accountBreadcrumbBuilder")
	private ResourceBreadcrumbBuilder accountBreadcrumbBuilder;
	@Resource(name = "enumerationService")
	private EnumerationService enumerationService;
	@Resource(name = "vetProductFacade")
	private VetProductFacade vetProductFacade;
	@Resource(name = "b2bContentPageBreadcrumbBuilder")
	private ContentPageBreadcrumbBuilder contentPageBreadcrumbBuilder;
	@Resource(name = "defaultMyAccountFacade")
	private DefaultMyAccountFacade defaultMyAccountFacade;
	@Resource(name = "customerEmailResolutionService")
	private GlanbiaCustomerEmailResolutionService glanbiaCustomerEmailResolutionService;

	@Resource(name = "modelService")
	protected ModelService modelService;

	@Resource(name = "bulkOrderFacade")
	private BulkOrderFacade bulkOrderFacade;

	@Resource(name = "agriAddressValidator")
	private AddressValidator addressValidator;

	@Resource(name = "myAccountDashboardFacade")
	private MyAccountDashboardFacade myAccountDashboardFacade;

	@Resource(name = "productSearchFacade")
	private ProductSearchFacade<ProductData> productSearchFacade;

	@Resource(name = "sapPriceCacheFacade")
	private SapPriceCacheFacade sapPriceCacheFacade;

	@Resource(name = "glanbiaCustomerSurveyFacade")
	private GlanbiaCustomerSurveyFacade glanbiaCustomerSurveyFacade;

	@Resource(name = "b2bCheckoutFacade")
	private de.hybris.platform.b2bacceleratorfacades.api.cart.CheckoutFacade b2bCheckoutFacade;

	@Resource(name = "cartFacade")
	private CartFacade cartFacade;

	@Resource(name = "milkCalendarDateFormatter")
	protected SimpleDateFormat milkCalendarDateFormatter;

	@Resource(name = "customerConsentDataStrategy")
	protected CustomerConsentDataStrategy customerConsentDataStrategy;

	@Resource(name = "emarsysContactDataService")
	protected EmarsysContactDataService emarsysContactDataService;

	@Resource(name = "defaultDeviceDetectionFacade")
	private DefaultDeviceDetectionFacade defaultDeviceDetectionFacade;

	@GetMapping
	@RequestMapping(method = RequestMethod.GET)
	@RequireHardLogIn
	public String account(final Model model, @RequestParam(value = WebserviceConstants.PDF_DERROR, required = false) boolean pdfDError) throws CMSItemNotFoundException
	{
		LOG.info("My account info start");
		if ((sessionService.getAttribute("user") instanceof VetCustomerModel) == false)
		{
			LOG.info("My account----> redirect login");
			return "redirect:/login";
		}
		final VetCustomerModel user =  sessionService.getAttribute("user");
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs(null));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		model.addAttribute(STATUS, user.getIsPaperlessOptIn());
		final List<String> harvestYears;
		harvestYears = DateUtil.getHarvestYears();
		model.addAttribute("harvestYears", harvestYears);
		String userGroup = myAccountDashboardFacade.getUerGroup(user).getUid();//retriving user type from user group
		String page;
		switch (userGroup){
			case AgriCoreConstants.MILK_UG:
				page = MILK_SUPPLIER;
				String supplierId = user.getMilkSupplierId();
				model.addAttribute(AgriCoreConstants.SUPPLIER_ID, supplierId);
				getQualityResultData(model,Config.getParameter(PREVIOUS_TEST_RESULTS_ROWS_COUNT));
				break;
			case AgriCoreConstants.TRADING_UG:
				page = TRAD_ACCOUNT;
				break;
			case AgriCoreConstants.MILK_TRAD_UG:
				page = MILKTRADING;
				supplierId = user.getMilkSupplierId();
				model.addAttribute(AgriCoreConstants.SUPPLIER_ID, supplierId);
				getQualityResultData(model, Config.getParameter(PREVIOUS_TEST_RESULTS_ROWS_COUNT));
				break;
			case AgriCoreConstants.NORTHERN_IRELAND_UG:
				page = NIR_USER;
				supplierId = user.getMilkSupplierId();
				model.addAttribute(AgriCoreConstants.SUPPLIER_ID, supplierId);
				getQualityResultData(model, Config.getParameter(PREVIOUS_TEST_RESULTS_ROWS_COUNT));
				break;
			case AgriCoreConstants.GRAIN_UG:
				page = GRAIN_SUPPLIER;
				break;
			case AgriCoreConstants.MILK_TRAD_GRAIN_UG:
				page = MILKTRADINGGRAIN;
				supplierId = user.getMilkSupplierId();
				model.addAttribute(AgriCoreConstants.SUPPLIER_ID, supplierId);
				getQualityResultData(model,Config.getParameter(PREVIOUS_TEST_RESULTS_ROWS_COUNT));
				break;
			case AgriCoreConstants.MILK_GRAIN_UG:
				page = MILKGRAIN;
				supplierId = user.getMilkSupplierId();
				model.addAttribute(AgriCoreConstants.SUPPLIER_ID, supplierId);
				getQualityResultData(model,Config.getParameter(PREVIOUS_TEST_RESULTS_ROWS_COUNT));
				break;
			case AgriCoreConstants.GRAIN_TRAD_UG:
				page = GRAIN_TRAD;
				break;
			case AgriCoreConstants.USERGROUP_EMP:
				page = STAFF;
				model.addAttribute(AgriCoreConstants.USERGROUP, AgriCoreConstants.USERGROUP_EMP);
				break;
			default:
				page = ONLINE_SHOPPER;
				model.addAttribute("userEmail", user.getEmail());
				break;

		}

		if (Config.getBoolean("myaccount.go.live.popup.show.flag", false)) {
			if (sessionService.getAttribute("showMyAccountGoLivePopup") == null) {
				sessionService.setAttribute("showMyAccountGoLivePopup", false);
				model.addAttribute("showMyAccountGoLivePopup", true);
			}
		}

		storeCmsPageInModel(model, getContentPageForLabelOrId(page));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(page));
		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));
		model.addAttribute(WebserviceConstants.PDF_DERROR, pdfDError);
		LOG.info("My account info end before return ----->");
		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTDASHBOARDPAGE;

	}

	@GetMapping("/edit-profile")
	@RequireHardLogIn
	public String editMyProfile(final Model model) throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(ACCOUNT_CMS_PAGE));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(ACCOUNT_CMS_PAGE));
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs(TEXT_ACCOUNT_PROFILE));

		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);

		final VetCustomerModel user = sessionService.getAttribute("user");

		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));
		return ControllerConstants.Views.Pages.Account.ACCOUNTEDITPROFILE;
	}

	@RequestMapping(value = "/orders", method = RequestMethod.GET)
	@RequireHardLogIn
	public String orders(@RequestParam(value = "page", defaultValue = "0") final int page,
			@RequestParam(value = "show", defaultValue = "Page") final ShowMode showMode,
			@RequestParam(value = "sort", required = false) final String sortCode, final Model model) throws CMSItemNotFoundException
	{
		// Get all order statuses except for the Pending Quote status
		final OrderStatus[] orderStatuses = {};
		// Handle paged search results
		final PageableData pageableData = createPageableData(page, 5, sortCode, showMode);
		final SearchPageData<OrderHistoryData> searchPageData = orderFacade.getPagedOrderHistoryForStatuses(pageableData,
				orderStatuses);
		final List<OrderHistoryData> oderList = new ArrayList<OrderHistoryData>();
		for (final OrderHistoryData od : searchPageData.getResults())
		{
			od.setPurchaseOrderNumber(myAccountDashboardFacade.getErpAccountNumber(od.getCode()));
			oderList.add(od);
		}
		searchPageData.setResults(oderList);
		populateModel(model, searchPageData, showMode);

		storeCmsPageInModel(model, getContentPageForLabelOrId(ORDER_HISTORY_CMS_PAGE));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(ORDER_HISTORY_CMS_PAGE));
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("text.account.orderHistory"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		model.addAttribute("purchases", Config.getParameter("dashboard.online.orders"));

		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");

		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));

		return ControllerConstants.Views.Pages.Account.ACCOUNTORDERHISTORYPAGE;
	}

	@RequestMapping(value = "/milk-statement-summary", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getMilkStatementsPage(final Model model, @RequestParam(value = WebserviceConstants.PDF_DERROR, required = false) boolean pdfDError) throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(MILK_STATEMENTS_SUMMARY));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(MILK_STATEMENTS_SUMMARY));

		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("milk.statements.summary"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		model.addAttribute("dashboard", Config.getParameter("dashboard.homepage.milk"));

		final VetCustomerModel user =sessionService.getAttribute("user");

		try
		{
			ServiceRespMapper serviceRespMapper = null;
			serviceRespMapper = defaultMyAccountFacade.getFarmDocuments(null, null);
			final List<ZArchive_Library> docList = serviceRespMapper.getzArchive_Library();
			for (final ZArchive_Library doc : docList)
			{
				if (doc.getDoc_type_id().equalsIgnoreCase(ZMKITSV001))
				{
					model.addAttribute("milkReturnSummary", doc);
					break;
				}
			}
		}
		catch (final Exception e)
		{
			LOG.error(e);
		}
		model.addAttribute(WebserviceConstants.PDF_DERROR, pdfDError);
		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTNONDASHBOARDPAGE;
	}

	@RequestMapping(value = "/peak-milk", method = RequestMethod.GET)
	@RequireHardLogIn
	public String peakMilk(final Model model) throws CMSItemNotFoundException
	{
		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");
		if (StringUtils.isNotBlank(user.getMilkSupplierId()))
		{
			model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("milk.peak.milk"));
			model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
			final LocalDate localDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			final String year = Integer.toString(localDate.getYear());
			final CPIServiceRespMapper results = defaultMyAccountFacade.getPeakMilkResults(year);

			final Optional<CPIServiceRespMapper> response = Optional.ofNullable(results);
			storeCmsPageInModel(model, getContentPageForLabelOrId(PEAKMILK));
			setUpMetaDataForContentPage(model, getContentPageForLabelOrId(PEAKMILK));
			if (response.isPresent())
			{
				response.get().getResult().getEt_peak_milk().forEach((result) -> {
					final String monthName = Month.of(Integer.parseInt(result.getPsd_period())).name();
					result.setPsd_year(monthName.substring(0, 1) + monthName.substring(1).toLowerCase() + " " + result.getPsd_year());
					model.addAttribute("peakMilkResult" + result.getPsd_period(), result);
					if (result.getPsd_curr_period().equalsIgnoreCase(WebserviceConstants.SAP_CPI.FLAG_YES))
					{
						model.addAttribute(CURRENT_MONTH, result.getPsd_period());
					}
				});
				Collections.reverse(response.get().getResult().getEt_peak_milk());
				model.addAttribute("results", response.get().getResult().getEt_peak_milk());
				if (model.getAttribute(CURRENT_MONTH) == null)
				{
					model.addAttribute(CURRENT_MONTH, response.get().getResult().getEt_peak_milk().get(0).getPsd_period());
				}
			}
			else
			{
				model.addAttribute("error", WebserviceConstants.SAP_CPI.FLAG_YES);
			}
			return ControllerConstants.Views.Pages.Account.PEAKMILKPAGE;
		}
		return REDIRECT_MY_ACCOUNT;

	}


	@RequestMapping(value = "/recent-transaction", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getRecentTransactionPage(final Model model) throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(RECENT_TRANSACTION_SUMMARY));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(RECENT_TRANSACTION_SUMMARY));

		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("nondashboard.recent.transactions"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		model.addAttribute("dashboard", Config.getParameter("dashboard.homepage.milk"));

		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");

		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));

		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTRECENTTRANSACTIONPAGE;
	}

	@RequestMapping(value = "/purchases-by-category", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getPurchasesByCategoryPage(final Model model) throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(PURCHASES_BY_CATEGORY));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(PURCHASES_BY_CATEGORY));

		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("nondashboard.purchasesBy.category"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		model.addAttribute("dashboard", Config.getParameter("dashboard.homepage.milk"));

		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");

		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));

		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTNONDASHBOARDPAGE;
	}

	@RequestMapping(value = "/purchases-by-type", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getPurchasesByTypePage(final Model model) throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(PURCHASES_BY_TYPE));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(PURCHASES_BY_TYPE));
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("nondashboard.purchasesBy.type"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);

		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));

		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTPURCHASESBYTYPEPAGE;
	}

	@RequestMapping(value="/gain-momentum-program",method=RequestMethod.GET)
	@RequireHardLogIn
	public String getGainMomentumProgramPage(final Model model) throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(GAIN_MOMENTUM_PROGRAMME));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(GAIN_MOMENTUM_PROGRAMME));
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("nondashboard.gainmomentum.program"));
		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTGAINMOMENTUMPROGRAMMEPAGE;
	}

	@RequestMapping(value = "/trading-account-summary", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getTradingStatementsPage(final Model model, @RequestParam(value = WebserviceConstants.PDF_DERROR, required = false) boolean pdfDError) throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(TRADING_STATEMENTS_SUMMARY));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(TRADING_STATEMENTS_SUMMARY));

		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("trading.statements.summary"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);

		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");

		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));
		model.addAttribute(STATUS, user.getIsPaperlessOptIn());
		model.addAttribute(PAPERLESS, PAPERLESS_OPTION_YES);
		model.addAttribute(WebserviceConstants.PDF_DERROR, pdfDError);
		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTTRADINGACCSUMMARYPAGE;
	}



	@RequestMapping(value = "/pay-trading-account", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getPayTradingAccountPage(final Model model, final HttpServletRequest request) throws CMSItemNotFoundException
	{

		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("pay.trading.account"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);

		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");

		if (StringUtils.isNotEmpty(request.getParameter("CART_ID")))
		{
			LOG.info("Account Payment Captured with realex ID: " + request.getParameter("CART_ID"));

			final String[] paymentIdpArray = request.getParameter("CART_ID").split("_");
			if (paymentIdpArray.length > 1)
			{
				model.addAttribute("completedPaymentAccount", paymentIdpArray[1]);
			}
		}


		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));

		storeCmsPageInModel(model, getContentPageForLabelOrId(PAY_TRADING_ACCOUNT));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(PAY_TRADING_ACCOUNT));

		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTPAYTRADINGACCOUNTPAGE;
	}

	@RequestMapping(value = "/milk-supply-forecast", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getMilkSupplyForecastPage(final Model model) throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(MILK_SUPPLY_FORECAST));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(MILK_SUPPLY_FORECAST));
		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");
		final String supplierId = user.getMilkSupplierId();

		model.addAttribute(AgriCoreConstants.SUPPLIER_ID, supplierId);
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("milk.supply.forecast"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		model.addAttribute("dashboard", Config.getParameter("dashboard.homepage.milk"));
		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));

		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTNONDASHBOARDPAGE;
	}

	@RequestMapping(value = "/grain-statements", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getGrainStatementsPage(final Model model) throws CMSItemNotFoundException
	{
		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");
		model.addAttribute("email", user.getEmail());

		storeCmsPageInModel(model, getContentPageForLabelOrId(GRAIN_STATEMENTS_SUMMARY));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(GRAIN_STATEMENTS_SUMMARY));

		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("grain.statements.summary"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));

		return ControllerConstants.Views.Pages.MyAccountPages.GRAINSTATEMENTPAGETEMPLATE;
	}

	@RequestMapping(value = "/supply-by-collection", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getSupplyCollectionPage(final Model model) throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(MILK_SUPPLY_COLLECTION));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(MILK_SUPPLY_COLLECTION));
		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");
		final String supplierId = user.getMilkSupplierId();


		model.addAttribute(AgriCoreConstants.SUPPLIER_ID, supplierId);
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("milk.supply.collection"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));

		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTNONDASHBOARDPAGE;
	}

	private String getValue(final String input)
	{
		return getMessageSource().getMessage(input, null, getI18nService().getCurrentLocale());
	}

	@RequestMapping(value = "/yearly-supply-and-quota", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getYearlySupplyAndQuota(final Model model) throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(YEARLY_SUPPLY_AND_QUOTA));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(YEARLY_SUPPLY_AND_QUOTA));
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("milk.supply.historic"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);

		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");
		final String supplierId = user.getMilkSupplierId();

		model.addAttribute(AgriCoreConstants.SUPPLIER_ID, supplierId);

		ServiceRespMapper headerServiceRespMapper = null;
		ServiceRespMapper serviceRespMapper = null;

		final String from = Config.getParameter("yearly.supply.quota.default");

		final int nextYear = Integer.parseInt(from) + 1;
		try
		{

			headerServiceRespMapper = defaultMyAccountFacade.getSupplierSumQuota(from);
			if (headerServiceRespMapper != null)
			{
				model.addAttribute("headrEtOutput", headerServiceRespMapper.getEt_Output());
				model.addAttribute("headrEtOutputTotal", headerServiceRespMapper.getTotal());
			}

			serviceRespMapper = defaultMyAccountFacade.getSupplyQuota(from);
			if (serviceRespMapper != null)
			{
				model.addAttribute("etOutput", serviceRespMapper.getEt_Output());
				model.addAttribute("yearlySupplyResult", serviceRespMapper);
				model.addAttribute("yearlySupplyResultJson", gson.toJson(serviceRespMapper));
				model.addAttribute("httpStatus", serviceRespMapper.getHttpStatus().name());
			}
		}
		catch (final Exception e)
		{
			LOG.error(" Error on AccountPageController.getYearlySupplyAndQuota() method and error is + " + e);
		}

		model.addAttribute("headrHears", ("" + from + "/" + nextYear));
		model.addAttribute("dashboard", Config.getParameter("dashboard.homepage.milk"));
		model.addAttribute("years", DateUtil.getPastYearsOnly(Integer.parseInt(from)));

		model.addAttribute("year", from);
		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));

		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTNONDASHBOARDPAGE;
	}

	@RequestMapping(value = "/my-supply-agreement", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getSupplyAgreementPage(final Model model) throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(POLICY_SUPPLY_AGREEMENT));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(POLICY_SUPPLY_AGREEMENT));
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("policy.supplyAgreement"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));
		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTPOLICYPAGE;
	}

	@RequestMapping(value = "/liquid-purchasing-policy", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getLiquidPurchasingPage(final Model model) throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(POLICY_LIQUID_PURCHASING));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(POLICY_LIQUID_PURCHASING));
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("policy.liquidPurchasing"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));
		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTPOLICYPAGE;
	}

	@RequestMapping(value = "/manufacturing-purchasing-policy", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getManufacturingPurchasingPage(final Model model) throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(POLICY_MANUFACTURING));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(POLICY_MANUFACTURING));
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("policy.manufacturingOurchasingPolicy"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));
		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTPOLICYPAGE;
	}

	@RequestMapping(value = "/fixed-pricing-scheme", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getFixedPricingPage(final Model model) throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(POLICY_FIXED_PRICE));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(POLICY_FIXED_PRICE));
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("policy.fixedPricingScheme"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));
		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTPOLICYPAGE;
	}

	@RequestMapping(value = "/quality-manual", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getQualityManualPage(final Model model) throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(POLICY_QUALITY_MANUAL));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(POLICY_QUALITY_MANUAL));
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("policy.qualityManual"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));

		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTPOLICYPAGE;
	}

	@RequestMapping(value = "/nir-policy", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getNirPolicyPage(final Model model) throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(POLICY_NIR));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(POLICY_NIR));
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("policy.supplyAgreement"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));
		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTPOLICYPAGE;
	}

	@RequestMapping(value = "/sustainability-manual", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getSustainabiltityManualPage(final Model model) throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(POLICY_SUSTAINABILITY_MANUAL));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(POLICY_SUSTAINABILITY_MANUAL));
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("policy.sustainabilityManual"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));

		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTPOLICYPAGE;
	}

	@RequestMapping(value = "/farm-development", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getFarmManualPage(final Model model) throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(POLICY_FARM_DEVELOPMENT));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(POLICY_FARM_DEVELOPMENT));
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("policy.farmDevelopmentManual"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));

		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTPOLICYPAGE;
	}

	@RequestMapping(value = "/order/" + ORDER_CODE_PATH_VARIABLE_PATTERN, method = RequestMethod.GET)
	@RequireHardLogIn
	public String order(@PathVariable("orderCode") final String orderCode, final Model model) throws CMSItemNotFoundException
	{
		try
		{
			final OrderData orderDetails = orderFacade.getOrderDetailsForCode(orderCode);
			orderDetails.setPurchaseOrderNumber(myAccountDashboardFacade.getErpAccountNumber(orderDetails.getCode()));
			addDeliveryCostToGrossPrice(orderDetails);
			finalPayableAmount(orderDetails);
			model.addAttribute("orderData", orderDetails);
			model.addAttribute(new ReorderForm());

			final List<Breadcrumb> breadcrumbs = accountBreadcrumbBuilder.getBreadcrumbs();
			breadcrumbs.add(new Breadcrumb("/my-account/orders",
					getMessageSource().getMessage("text.account.orderHistory", null, getI18nService().getCurrentLocale()), null));
			breadcrumbs.add(new Breadcrumb("#", getMessageSource().getMessage("text.account.order.orderBreadcrumb", new Object[]
			{ orderDetails.getPurchaseOrderNumber() }, "Order {0}", getI18nService().getCurrentLocale()), ControllerConstants.Variables.ACTIVE));
			model.addAttribute(BREADCRUMBS, breadcrumbs);

		}
		catch (final UnknownIdentifierException e)
		{
			LOG.warn("Attempted to load a order that does not exist or is not visible", e);
			return REDIRECT_MY_ACCOUNT;
		}
		storeCmsPageInModel(model, getContentPageForLabelOrId(ORDER_DETAIL_CMS_PAGE));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(ORDER_DETAIL_CMS_PAGE));

		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");

		model.addAttribute("purchases", Config.getParameter("dashboard.online.orders"));
		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));

		return ControllerConstants.Views.Pages.Account.ACCOUNTORDERPAGE;
	}

	@Override
	public void finalPayableAmount(final AbstractOrderData abstractOrderData)
	{
		final BigDecimal orderDiscount = abstractOrderData.getOrderDiscounts().getValue();
		final BigDecimal grossPriceWithDeliveryCost = abstractOrderData.getGrossPriceWithDeliveryCost().getValue();
		BigDecimal finalAmountPayable;
		LOG.info("orderDiscount is" + orderDiscount);
		LOG.info("grossPriceWithDeliveryCost is" + grossPriceWithDeliveryCost);
		if (orderDiscount.doubleValue() > 0.0)
		{
			finalAmountPayable = BigDecimal.valueOf(0d).add(grossPriceWithDeliveryCost).subtract(orderDiscount);
		}
		else
		{
			finalAmountPayable = BigDecimal.valueOf(0d).add(grossPriceWithDeliveryCost);
		}
		finalAmountPayable = finalAmountPayable.setScale(2, BigDecimal.ROUND_HALF_DOWN);
		final PriceData totalAmountPayable = getPriceDataFactory().create(PriceDataType.BUY, finalAmountPayable, "EUR");
		abstractOrderData.setTotalPrice(totalAmountPayable);
	}

	private OrderData addDeliveryCostToGrossPrice(final OrderData orderData)
	{
		final BigDecimal grossPrice = orderData.getSubTotal().getValue();
		BigDecimal deliveryCost;
		BigDecimal grossValueWithDelivery = null;
		if (orderData.getDeliveryCost() != null)
		{
			deliveryCost = orderData.getDeliveryCost().getValue();
			grossValueWithDelivery = BigDecimal.valueOf(0d).add(grossPrice).add(deliveryCost);
		}
		else
		{
			grossValueWithDelivery = BigDecimal.valueOf(0d).add(grossPrice);
		}
		final PriceData grossPriceData = getPriceDataFactory().create(PriceDataType.BUY, grossValueWithDelivery, "EUR");
		orderData.setGrossPriceWithDeliveryCost(grossPriceData);
		return orderData;
	}

	@RequestMapping(value = "/my-purchased-products", method = RequestMethod.GET)
	public String productCategory(final Model model) throws CMSItemNotFoundException
	{
		OrderData orderDetails;
		List<OrderEntryData> entries;
		final SetMultimap<String, List> multiMap = HashMultimap.create();

		final OrderStatus[] statuses = {};

		final List<OrderHistoryData> ordHis = orderFacade.getOrderHistoryForStatuses(statuses);

		for (final OrderHistoryData order : ordHis)
		{

			orderDetails = orderFacade.getOrderDetailsForCode(order.getCode());

			orderDetails.setPurchaseOrderNumber(myAccountDashboardFacade.getErpAccountNumber(order.getCode()));

			entries = orderDetails.getEntries();
			ProductData vetProductData = null;
			for (final OrderEntryData entry : entries)
			{
				try
				{
					vetProductData = vetProductFacade.getProductCategorForCode(entry.getProduct().getCode());
				}
				catch (final Exception e)
				{
					//skip this product in case of exceptions like Product code not found
					LOG.error("Product code not found" + ExceptionUtils.getFullStackTrace(e));
				}
				if (vetProductData != null)
				{
					final List<CategoryData> categoryDataList = (List<CategoryData>) vetProductData.getCategories();

					for (final CategoryData categoryData : categoryDataList)
					{
						multiMap.put(categoryData.getName(),
								Arrays.asList(entry.getProduct(), orderDetails.getPurchaseOrderNumber(), orderDetails.getCreated()));
					}
				}
				vetProductData = null;
			}

		}

		model.addAttribute("categoryProd", multiMap.asMap());

		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("text.account.mypurchasedorders"));
		storeCmsPageInModel(model, getContentPageForLabelOrId(MY_PURCHASED_PRODUCTS_CMS_PAGE));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(MY_PURCHASED_PRODUCTS_CMS_PAGE));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		model.addAttribute("purchases", Config.getParameter("dashboard.products"));

		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");

		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));

		return ControllerConstants.Views.Pages.Account.ACCOUNTPURCHASEDPRODUCT;
	}

	@RequestMapping(value = "/profile", method = RequestMethod.GET)
	@RequireHardLogIn
	public String profile(final Model model) throws CMSItemNotFoundException
	{
		final List<TitleData> titles = userFacade.getTitles();

		final CustomerData customerData = customerFacade.getCurrentCustomer();

		if (customerData.getTitleCode() != null)
		{
			model.addAttribute("title", CollectionUtils.find(titles, new Predicate()
			{
				@Override
				public boolean evaluate(final Object object)
				{
					if (object instanceof TitleData)
					{
						return customerData.getTitleCode().equals(((TitleData) object).getCode());
					}
					return false;
				}
			}));
		}

		model.addAttribute("customerData", customerData);
		model.addAttribute("emailFromUid", glanbiaCustomerEmailResolutionService.getEmailFromUid(customerData.getDisplayUid()));
		storeCmsPageInModel(model, getContentPageForLabelOrId(PROFILE_CMS_PAGE));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(PROFILE_CMS_PAGE));
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs(TEXT_ACCOUNT_PROFILE));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);

		final VetCustomerModel user =  sessionService.getAttribute("user");
		if (showPaperlessOptIn(user) && myAccountDashboardFacade.getNorthernIrelandAccount(user, AgriCoreConstants.NIR_ACCOUNT) == null) {
			model.addAttribute(PAPERLESS, user.getIsPaperlessOptIn());
		}else{
			model.addAttribute(NO_PAPERLESS, Boolean.TRUE);
		}
		model.addAttribute("optIn", user.getIsEmailOptIn());
		addRegistrationConsentDataToModel(model);
		return ControllerConstants.Views.Pages.Account.ACCOUNTPROFILEPAGE;
	}

	@RequestMapping(value = "/update-email", method = RequestMethod.GET)
	@RequireHardLogIn
	public String editEmail(final Model model) throws CMSItemNotFoundException
	{
		final CustomerData customerData = customerFacade.getCurrentCustomer();
		final UpdateEmailForm updateEmailForm = new UpdateEmailForm();

		updateEmailForm.setEmail(glanbiaCustomerEmailResolutionService.getEmailFromUid(customerData.getDisplayUid()));

		model.addAttribute("updateEmailForm", updateEmailForm);

		storeCmsPageInModel(model, getContentPageForLabelOrId(PROFILE_CMS_PAGE));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(PROFILE_CMS_PAGE));
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs(TEXT_ACCOUNT_PROFILE));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);

		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");

		model.addAttribute("my_account", Config.getParameter("dashboard.editprofile"));
		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));
		return ControllerConstants.Views.Pages.Account.ACCOUNTPROFILEEMAILEDITPAGE;
	}

	@RequestMapping(value = "/update-email", method = RequestMethod.POST)
	@RequireHardLogIn
	public String updateEmail(@Valid final UpdateEmailForm updateEmailForm, final BindingResult bindingResult, final Model model,
			final RedirectAttributes redirectAttributes) throws CMSItemNotFoundException
	{
		String returnAction = REDIRECT_TO_PROFILE_PAGE;

		if (!updateEmailForm.getEmail().equals(updateEmailForm.getChkEmail()))
		{
			bindingResult.rejectValue("chkEmail", "validation.checkEmail.equals", new Object[] {}, "validation.checkEmail.equals");
		}

		if (bindingResult.hasErrors())
		{
			returnAction = errorUpdatingEmail(model);
		}
		else
		{
			try
			{

				customerFacade.changeUid(glanbiaCustomerEmailResolutionService.getUserUidFromEmail(updateEmailForm.getEmail()),
						updateEmailForm.getPassword());
				GlobalMessages.addFlashMessage(redirectAttributes, GlobalMessages.CONF_MESSAGES_HOLDER,
						"text.account.profile.confirmationUpdated");

				// Replace the spring security authentication with the new UID
				final String newUid = customerFacade.getCurrentCustomer().getUid().toLowerCase();
				final Authentication oldAuthentication = SecurityContextHolder.getContext().getAuthentication();
				final UsernamePasswordAuthenticationToken newAuthentication = new UsernamePasswordAuthenticationToken(newUid, null,
						oldAuthentication.getAuthorities());
				newAuthentication.setDetails(oldAuthentication.getDetails());
				SecurityContextHolder.getContext().setAuthentication(newAuthentication);
			}
			catch (final DuplicateUidException e)
			{
				bindingResult.rejectValue("email", "profile.email.unique");
				returnAction = errorUpdatingEmail(model);
				LOG.info(e);
			}
			catch (final PasswordMismatchException passwordMismatchException)
			{
				bindingResult.rejectValue("email", "profile.currentPassword.invalid");
				returnAction = errorUpdatingEmail(model);
				LOG.info(passwordMismatchException);
			}
		}

		return returnAction;
	}

	protected String errorUpdatingEmail(final Model model) throws CMSItemNotFoundException
	{
		final String returnAction;
		GlobalMessages.addErrorMessage(model, "form.global.error");
		storeCmsPageInModel(model, getContentPageForLabelOrId(PROFILE_CMS_PAGE));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(PROFILE_CMS_PAGE));
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs(TEXT_ACCOUNT_PROFILE));
		returnAction = ControllerConstants.Views.Pages.Account.ACCOUNTPROFILEEMAILEDITPAGE;
		return returnAction;
	}

	@RequestMapping(value = "/update-profile", method = RequestMethod.GET)
	@RequireHardLogIn
	public String editProfile(final Model model) throws CMSItemNotFoundException
	{
		model.addAttribute("titleData", userFacade.getTitles());

		final CustomerData customerData = customerFacade.getCurrentCustomer();
		final UpdateProfileForm updateProfileForm = new UpdateProfileForm();

		updateProfileForm.setTitleCode(customerData.getTitleCode());
		updateProfileForm.setFirstName(customerData.getFirstName());
		updateProfileForm.setLastName(customerData.getLastName());

		model.addAttribute("updateProfileForm", updateProfileForm);

		storeCmsPageInModel(model, getContentPageForLabelOrId(PROFILE_CMS_PAGE));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(PROFILE_CMS_PAGE));

		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs(TEXT_ACCOUNT_PROFILE));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);

		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");

		model.addAttribute("my_account", Config.getParameter("dashboard.editprofile"));
		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));

		return ControllerConstants.Views.Pages.Account.ACCOUNTPROFILEEDITPAGE;
	}

	@RequestMapping(value = "/update-profile", method = RequestMethod.POST)
	@RequireHardLogIn
	public String updateProfile(@Valid final UpdateProfileForm updateProfileForm, final BindingResult bindingResult,
			final Model model, final RedirectAttributes redirectAttributes) throws CMSItemNotFoundException
	{
		String returnAction = ControllerConstants.Views.Pages.Account.ACCOUNTPROFILEEDITPAGE;
		final CustomerData currentCustomerData = customerFacade.getCurrentCustomer();
		final CustomerData customerData = new CustomerData();
		customerData.setTitleCode(updateProfileForm.getTitleCode());
		customerData.setFirstName(updateProfileForm.getFirstName());
		customerData.setLastName(updateProfileForm.getLastName());
		customerData.setUid(currentCustomerData.getUid());
		customerData.setDisplayUid(currentCustomerData.getDisplayUid());

		model.addAttribute("titleData", userFacade.getTitles());

		if (bindingResult.hasErrors())
		{
			GlobalMessages.addErrorMessage(model, "form.global.error");
		}

		else
		{
			try
			{
				customerFacade.updateProfile(customerData);
				triggerEmarsysContactCUEvent(WebserviceConstants.Emarsys.CONTACT_UPDATE, getVetCustomerData());
				GlobalMessages.addFlashMessage(redirectAttributes, GlobalMessages.CONF_MESSAGES_HOLDER,
						"text.account.profile.confirmationUpdated");
				returnAction = REDIRECT_TO_PROFILE_PAGE;
			}
			catch (final DuplicateUidException e)
			{
				bindingResult.rejectValue("email", "registration.error.account.exists.title");
				GlobalMessages.addErrorMessage(model, "form.global.error");
				LOG.info(e);
			}
		}

		storeCmsPageInModel(model, getContentPageForLabelOrId(PROFILE_CMS_PAGE));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(PROFILE_CMS_PAGE));
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs(TEXT_ACCOUNT_PROFILE));
		return returnAction;
	}

	@RequestMapping(value = "/update-password", method = RequestMethod.GET)
	@RequireHardLogIn
	public String updatePassword(final Model model,final HttpServletRequest request) throws CMSItemNotFoundException
	{
		final UpdatePasswordForm updatePasswordForm = new UpdatePasswordForm();

		model.addAttribute("updatePasswordForm", updatePasswordForm);

		storeCmsPageInModel(model, getContentPageForLabelOrId(PROFILE_CMS_PAGE));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(PROFILE_CMS_PAGE));

		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("text.account.profile.updatePasswordForm"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);

		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");
		model.addAttribute("isPasswordPolicyUpdate", (BooleanUtils.isTrue((Boolean) request.getSession(false).getAttribute("isPasswordPolicyUpdate")) && "MD5".equalsIgnoreCase(user.getPasswordEncoding())));

		model.addAttribute("my_account", Config.getParameter("dashboard.editprofile"));
		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));
		return ControllerConstants.Views.Pages.Account.ACCOUNTCHANGEPASSWORDPAGE;
	}

	@RequestMapping(value = "/update-password", method = RequestMethod.POST)
	@RequireHardLogIn
	public String updatePassword(@Valid final UpdatePasswordForm updatePasswordForm, final BindingResult bindingResult,
			final Model model, final RedirectAttributes redirectAttributes) throws CMSItemNotFoundException
	{

		if (!bindingResult.hasErrors())
		{
			if (updatePasswordForm.getNewPassword().equals(updatePasswordForm.getCheckNewPassword()))
			{
				try
				{
					customerFacade.changePassword(updatePasswordForm.getCurrentPassword(), updatePasswordForm.getNewPassword());
					// TIR-1514 : Remove update md5 password group from customer (if available) when they update their password.
					vetUserFacade.removeUserGroupForCurrentCustomer(AgriCoreConstants.UPDATE_MD5_PWD_UG);
				}
				catch (final PasswordMismatchException localException)
				{
					bindingResult.rejectValue("currentPassword", "profile.currentPassword.invalid", new Object[] {},
							"profile.currentPassword.invalid");
					LOG.info(localException);
				}

			}
			else
			{
				bindingResult.rejectValue("checkNewPassword", "validation.checkPwd.equals", new Object[] {},
						"validation.checkPwd.equals");
			}
		}

		if (bindingResult.hasErrors())
		{
			GlobalMessages.addErrorMessage(model, "form.global.error");
			storeCmsPageInModel(model, getContentPageForLabelOrId(PROFILE_CMS_PAGE));
			setUpMetaDataForContentPage(model, getContentPageForLabelOrId(PROFILE_CMS_PAGE));

			model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("text.account.profile.updatePasswordForm"));
			model.addAttribute("my_account", Config.getParameter("dashboard.editprofile"));
			return ControllerConstants.Views.Pages.Account.ACCOUNTCHANGEPASSWORDPAGE;
		}
		else
		{
			GlobalMessages.addFlashMessage(redirectAttributes, GlobalMessages.CONF_MESSAGES_HOLDER,
					"text.account.confirmation.password.updated");
			return REDIRECT_TO_PROFILE_PAGE;
		}
	}

	@RequestMapping(value = "/address-book", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getAddressBook(final Model model) throws CMSItemNotFoundException
	{
		model.addAttribute("addressData", userFacade.getAddressBook());

		storeCmsPageInModel(model, getContentPageForLabelOrId(ADDRESS_BOOK_CMS_PAGE));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(ADDRESS_BOOK_CMS_PAGE));
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs(TEXT_ACCOUNT_ADDRESSBOOK));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");

		model.addAttribute("my_account", Config.getParameter("dashboard.addressbook"));
		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));
		return ControllerConstants.Views.Pages.Account.ACCOUNTADDRESSBOOKPAGE;
	}

	@RequestMapping(value = "/add-address", method = RequestMethod.GET)
	@RequireHardLogIn
	public String addAddress(final Model model) throws CMSItemNotFoundException
	{
		model.addAttribute("countryData", checkoutFacade.getDeliveryCountries());
		model.addAttribute("countyData", i18NFacade.getRegionsForCountryIso("IE"));
		model.addAttribute("titleData", userFacade.getTitles());
		model.addAttribute("addressForm", new AddressForm());
		model.addAttribute("addressBookEmpty", Boolean.valueOf(userFacade.isAddressBookEmpty()));

		setUpAccountAddressEditPage(model);

		return ControllerConstants.Views.Pages.Account.ACCOUNTEDITADDRESSPAGE;
	}

	@RequestMapping(value = "/add-address", method = RequestMethod.POST)
	@RequireHardLogIn
	public String addAddress(@Valid final AddressForm addressForm, final BindingResult bindingResult, final Model model,
			final HttpServletRequest request, final RedirectAttributes redirectModel) throws CMSItemNotFoundException
	{

		addressValidator.addressBookValidator(addressForm, bindingResult);
		if (bindingResult.hasErrors())
		{
			GlobalMessages.addErrorMessage(model, "form.global.error");
			storeCmsPageInModel(model, getContentPageForLabelOrId(ADD_EDIT_ADDRESS_CMS_PAGE));
			setUpMetaDataForContentPage(model, getContentPageForLabelOrId(ADD_EDIT_ADDRESS_CMS_PAGE));
			model.addAttribute("countryData", checkoutFacade.getDeliveryCountries());
			model.addAttribute("countyData", i18NFacade.getRegionsForCountryIso("IE"));

			model.addAttribute("titleData", userFacade.getTitles());
			final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");

			model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));

			final List<Breadcrumb> breadcrumbs = accountBreadcrumbBuilder.getBreadcrumbs(null);
			breadcrumbs.add(new Breadcrumb(ADDRESS_BOOK,
					getMessageSource().getMessage(TEXT_ACCOUNT_ADDRESSBOOK, null, getI18nService().getCurrentLocale()), null));
			breadcrumbs.add(new Breadcrumb("#", getMessageSource().getMessage("text.account.addressBook.addEditAddress", null,
					getI18nService().getCurrentLocale()), null));
			model.addAttribute(BREADCRUMBS, breadcrumbs);
			model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);

			return ControllerConstants.Views.Pages.Account.ACCOUNTEDITADDRESSPAGE;
		}

		final VetAddressData newAddress = new VetAddressData();
		newAddress.setTitleCode(addressForm.getTitleCode());
		newAddress.setFullName(addressForm.getFullName());
		newAddress.setLine1(addressForm.getLine1());
		newAddress.setLine2(addressForm.getLine2());
		newAddress.setLine3(addressForm.getLine3());
		newAddress.setEmail(addressForm.getEmailAddress());
		newAddress.setContactNumber(addressForm.getContactNumber());
		newAddress.setPhone(addressForm.getContactNumber());
		newAddress.setTown(addressForm.getTownCity());
		newAddress.setPostalCode(addressForm.getPostcode());
		newAddress.setBillingAddress(false);
		newAddress.setShippingAddress(true);
		newAddress.setVisibleInAddressBook(true);

		final CountryData countryData = new CountryData();
		countryData.setIsocode(addressForm.getCountryIso());
		newAddress.setCountry(countryData);

		final RegionData regionData = new RegionData();
		regionData.setIsocode(addressForm.getCounty());
		newAddress.setRegion(regionData);

		if (userFacade.isAddressBookEmpty())
		{
			newAddress.setDefaultAddress(true);
		}
		else
		{
			newAddress.setDefaultAddress(addressForm.getDefaultAddress().booleanValue());
		}
		vetUserFacade.addAddressWithoutReturningPK(newAddress);
		GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER, "account.confirmation.address.added");

		return REDIRECT_TO_ADDRESS_BOOK_PAGE;
	}

	@RequestMapping(value = "/edit-address/" + ADDRESS_CODE_PATH_VARIABLE_PATTERN, method = RequestMethod.GET)
	@RequireHardLogIn
	public String editAddress(@PathVariable("addressCode") final String addressCode, final Model model)
			throws CMSItemNotFoundException
	{
		final AddressForm addressForm = new AddressForm();
		model.addAttribute("countryData", checkoutFacade.getDeliveryCountries());
		model.addAttribute("countyData", i18NFacade.getRegionsForCountryIso("IE"));
		model.addAttribute("titleData", userFacade.getTitles());
		model.addAttribute("addressForm", addressForm);
		model.addAttribute("addressBookEmpty", Boolean.valueOf(userFacade.isAddressBookEmpty()));

		for (final VetAddressData addressData : vetUserFacade.getVetAddressBook())
		{
			if (addressData.getId() != null && addressData.getId().equals(addressCode))
			{
				model.addAttribute("addressData", addressData);
				addressForm.setAddressId(addressData.getId());
				addressForm.setTitleCode(addressData.getTitleCode());
				addressForm.setEmailAddress(addressData.getEmail());
				addressForm.setFullName(addressData.getFullName());
				addressForm.setLine1(addressData.getLine1());
				addressForm.setLine2(addressData.getLine2());
				addressForm.setLine3(addressData.getLine3());
				//addressForm.setContactNumber(addressData.getContactNumber());
				addressForm.setContactNumber(addressData.getPhone());
				addressForm.setTownCity(addressData.getTown());
				addressForm.setPostcode(addressData.getPostalCode());
				addressForm.setCounty(addressData.getRegion().getIsocode());
				addressForm.setCountryIso(addressData.getCountry().getIsocode());
				if (userFacade.getDefaultAddress() != null && userFacade.getDefaultAddress().getId() != null
						&& userFacade.getDefaultAddress().getId().equals(addressData.getId()))
				{
					addressForm.setDefaultAddress(Boolean.TRUE);
				}
				break;
			}
		}

		setUpAccountAddressEditPage(model);

		return ControllerConstants.Views.Pages.Account.ACCOUNTEDITADDRESSPAGE;
	}

	@RequestMapping(value = "/edit-address/" + ADDRESS_CODE_PATH_VARIABLE_PATTERN, method = RequestMethod.POST)
	@RequireHardLogIn
	public String editAddress(@Valid final AddressForm addressForm, final BindingResult bindingResult, final Model model,
			final RedirectAttributes redirectModel) throws CMSItemNotFoundException
	{
		addressValidator.addressBookValidator(addressForm, bindingResult);
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		if (bindingResult.hasErrors())
		{
			GlobalMessages.addErrorMessage(model, "form.global.error");
			storeCmsPageInModel(model, getContentPageForLabelOrId(ADD_EDIT_ADDRESS_CMS_PAGE));
			setUpMetaDataForContentPage(model, getContentPageForLabelOrId(ADD_EDIT_ADDRESS_CMS_PAGE));
			model.addAttribute("countryData", checkoutFacade.getDeliveryCountries());
			model.addAttribute("countyData", i18NFacade.getRegionsForCountryIso("IE"));
			model.addAttribute("titleData", userFacade.getTitles());

			final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");


			final List<Breadcrumb> breadcrumbs = accountBreadcrumbBuilder.getBreadcrumbs(null);
			breadcrumbs.add(new Breadcrumb(ADDRESS_BOOK,
					getMessageSource().getMessage(TEXT_ACCOUNT_ADDRESSBOOK, null, getI18nService().getCurrentLocale()), null));
			breadcrumbs.add(new Breadcrumb("#", getMessageSource().getMessage("text.account.addressBook.addEditAddress", null,
					getI18nService().getCurrentLocale()), null));
			model.addAttribute(BREADCRUMBS, breadcrumbs);
			model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
			model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));

			return ControllerConstants.Views.Pages.Account.ACCOUNTEDITADDRESSPAGE;
		}

		final VetAddressData newAddress = new VetAddressData();
		newAddress.setId(addressForm.getAddressId());
		newAddress.setTitleCode(addressForm.getTitleCode());
		newAddress.setFullName(addressForm.getFullName());
		newAddress.setEmail(addressForm.getEmailAddress());
		newAddress.setLine1(addressForm.getLine1());
		newAddress.setLine2(addressForm.getLine2());
		newAddress.setLine3(addressForm.getLine3());
		//newAddress.setContactNumber(addressForm.getContactNumber());
		newAddress.setPhone(addressForm.getContactNumber());
		newAddress.setTown(addressForm.getTownCity());
		newAddress.setPostalCode(addressForm.getPostcode());
		newAddress.setBillingAddress(false);
		newAddress.setShippingAddress(true);
		newAddress.setVisibleInAddressBook(true);

		final CountryData countryData = new CountryData();
		countryData.setIsocode(addressForm.getCountryIso());
		newAddress.setCountry(countryData);

		final RegionData regionData = new RegionData();
		regionData.setIsocode(addressForm.getCounty());
		newAddress.setRegion(regionData);

		if (Boolean.TRUE.equals(addressForm.getDefaultAddress()) || userFacade.getAddressBook().size() <= 1)
		{
			newAddress.setDefaultAddress(true);
		}
		else
		{
			newAddress.setDefaultAddress(addressForm.getDefaultAddress() != null && addressForm.getDefaultAddress().booleanValue());
		}
		vetUserFacade.editAddressWithoutReturningPK(newAddress);

		GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER,
				"text.account.addressBook.confirmationUpdated");
		return REDIRECT_TO_ADDRESS_BOOK_PAGE;
	}

	@RequestMapping(value = "/remove-address/" + ADDRESS_CODE_PATH_VARIABLE_PATTERN, method = RequestMethod.GET)
	@RequireHardLogIn
	public String removeAddress(@PathVariable("addressCode") final String addressCode, final RedirectAttributes redirectModel)
	{
		final AddressData addressData = new AddressData();
		addressData.setId(addressCode);
		userFacade.removeAddress(addressData);
		GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER, "account.confirmation.address.removed");
		return REDIRECT_TO_ADDRESS_BOOK_PAGE;
	}

	@RequestMapping(value = "/set-default-address/" + ADDRESS_CODE_PATH_VARIABLE_PATTERN, method = RequestMethod.GET)
	@RequireHardLogIn
	public String setDefaultAddress(@PathVariable("addressCode") final String addressCode, final RedirectAttributes redirectModel)
	{
		final AddressData addressData = new AddressData();
		addressData.setDefaultAddress(true);
		addressData.setVisibleInAddressBook(true);
		addressData.setId(addressCode);
		userFacade.setDefaultAddress(addressData);
		GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER,
				"account.confirmation.default.address.changed");
		return REDIRECT_TO_ADDRESS_BOOK_PAGE;
	}

	@RequestMapping(value = "/payment-details", method = RequestMethod.GET)
	@RequireHardLogIn
	public String paymentDetails(final Model model) throws CMSItemNotFoundException
	{
		model.addAttribute("customerData", customerFacade.getCurrentCustomer());
		model.addAttribute("paymentInfoData", userFacade.getCCPaymentInfos(true));
		storeCmsPageInModel(model, getContentPageForLabelOrId(PAYMENT_DETAILS_CMS_PAGE));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(ADD_EDIT_ADDRESS_CMS_PAGE));
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("text.account.paymentDetails"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);

		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");

		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));

		return ControllerConstants.Views.Pages.Account.ACCOUNTPAYMENTINFOPAGE;
	}

	@RequestMapping(value = "/set-default-payment-details", method = RequestMethod.POST)
	@RequireHardLogIn
	public String setDefaultPaymentDetails(@RequestParam final String paymentInfoId)
	{
		CCPaymentInfoData paymentInfoData = null;
		if (StringUtils.isNotBlank(paymentInfoId))
		{
			paymentInfoData = userFacade.getCCPaymentInfoForCode(paymentInfoId);
		}
		userFacade.setDefaultPaymentInfo(paymentInfoData);
		return REDIRECT_TO_PAYMENT_INFO_PAGE;
	}

	@RequestMapping(value = "/remove-payment-method", method = RequestMethod.POST)
	@RequireHardLogIn
	public String removePaymentMethod(final Model model, @RequestParam(value = "paymentInfoId") final String paymentMethodId,
			final RedirectAttributes redirectAttributes)
	{
		userFacade.unlinkCCPaymentInfo(paymentMethodId);
		GlobalMessages.addFlashMessage(redirectAttributes, GlobalMessages.CONF_MESSAGES_HOLDER,
				"text.account.profile.paymentCart.removed");
		return REDIRECT_TO_PAYMENT_INFO_PAGE;
	}

	@RequestMapping(value = "/orderApprovalDetails/" + WORKFLOW_ACTION_CODE_PATH_VARIABLE_PATTERN, method = RequestMethod.GET)
	@RequireHardLogIn
	public String orderApprovalDetails(@PathVariable("workflowActionCode") final String workflowActionCode, final Model model)
			throws CMSItemNotFoundException
	{
		try
		{
			final B2BOrderApprovalData orderApprovalDetails = orderFacade.getOrderApprovalDetailsForCode(workflowActionCode);
			model.addAttribute("orderApprovalData", orderApprovalDetails);
			if (!model.containsAttribute("orderApprovalDecisionForm"))
			{
				model.addAttribute("orderApprovalDecisionForm", new OrderApprovalDecisionForm());
			}

			final List<Breadcrumb> breadcrumbs = accountBreadcrumbBuilder.getBreadcrumbs(null);
			breadcrumbs.add(new Breadcrumb("/my-account/approval-dashboard",
					getMessageSource().getMessage("text.account.orderApprovalDashboard", null, getI18nService().getCurrentLocale()),
					null));
			breadcrumbs.add(new Breadcrumb("#", getMessageSource().getMessage("text.account.order.orderBreadcrumb", new Object[]
			{ orderApprovalDetails.getB2bOrderData().getCode() }, "Order {0}", getI18nService().getCurrentLocale()), null));

			model.addAttribute(BREADCRUMBS, breadcrumbs);

		}
		catch (final UnknownIdentifierException e)
		{
			LOG.warn("Attempted to load a order that does not exist or is not visible", e);
			return REDIRECT_MY_ACCOUNT;
		}
		storeCmsPageInModel(model, getContentPageForLabelOrId(ORDER_DETAIL_CMS_PAGE));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(ORDER_DETAIL_CMS_PAGE));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		return ControllerConstants.Views.Pages.Account.ACCOUNTORDERAPPROVALDETAILSPAGE;
	}

	@RequestMapping(value = "/order/approvalDecision", method = RequestMethod.POST)
	@RequireHardLogIn
	public String orderApprovalDecision(
			@ModelAttribute("orderApprovalDecisionForm") final OrderApprovalDecisionForm orderApprovalDecisionForm,
			final Model model) throws CMSItemNotFoundException
	{
		try
		{
			if ("REJECT".contains(orderApprovalDecisionForm.getApproverSelectedDecision())
					&& StringUtils.isEmpty(orderApprovalDecisionForm.getComments()))
			{
				GlobalMessages.addErrorMessage(model, "text.account.orderApproval.addApproverComments");
				model.addAttribute("orderApprovalDecisionForm", orderApprovalDecisionForm);
				return orderApprovalDetails(orderApprovalDecisionForm.getWorkFlowActionCode(), model);
			}

			final B2BOrderApprovalData b2bOrderApprovalData = new B2BOrderApprovalData();
			b2bOrderApprovalData.setSelectedDecision(orderApprovalDecisionForm.getApproverSelectedDecision());
			b2bOrderApprovalData.setApprovalComments(orderApprovalDecisionForm.getComments());
			b2bOrderApprovalData.setWorkflowActionModelCode(orderApprovalDecisionForm.getWorkFlowActionCode());

			orderFacade.setOrderApprovalDecision(b2bOrderApprovalData);

		}
		catch (final UnknownIdentifierException e)
		{
			LOG.warn("Attempted to load a order that does not exist or is not visible", e);
			return REDIRECT_MY_ACCOUNT;
		}

		return REDIRECT_MY_ACCOUNT + "/orderApprovalDetails/" + orderApprovalDecisionForm.getWorkFlowActionCode();
	}

	protected void setUpCommentIsEmptyError(final QuoteOrderForm quoteOrderForm, final Model model) throws CMSItemNotFoundException
	{
		quoteOrderForm.setNegotiateQuote(true);
		model.addAttribute("quoteOrderDecisionForm", quoteOrderForm);
		GlobalMessages.addErrorMessage(model, "text.quote.empty");
	}

	@RequestMapping(value = "/approval-dashboard", method = RequestMethod.GET)
	@RequireHardLogIn
	public String orderApprovalDashboard(@RequestParam(value = "page", defaultValue = "0") final int page,
			@RequestParam(value = "show", defaultValue = "Page") final ShowMode showMode,
			@RequestParam(value = "sort", required = false) final String sortCode, final Model model) throws CMSItemNotFoundException
	{
		final PageableData pageableData = createPageableData(page, 5, sortCode, showMode);
		final SearchPageData<? extends B2BOrderApprovalData> searchPageData = orderFacade
				.getPagedOrdersForApproval(new WorkflowActionType[]
				{ WorkflowActionType.START }, pageableData);
		populateModel(model, searchPageData, showMode);

		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("text.account.orderApprovalDashboard"));
		storeCmsPageInModel(model, getContentPageForLabelOrId(ORDER_APPROVAL_DASHBOARD_CMS_PAGE));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(ORDER_APPROVAL_DASHBOARD_CMS_PAGE));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		return ControllerConstants.Views.Pages.Account.ACCOUNTORDERAPPROVALDASHBOARDPAGE;
	}

	@RequestMapping(value = "/export-farmpackage", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getExportFarmPackagePage(final Model model) throws CMSItemNotFoundException
	{
		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");
		model.addAttribute("email", user.getEmail());

		storeCmsPageInModel(model, getContentPageForLabelOrId(EXPORT_FARM_PACKAGE));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(EXPORT_FARM_PACKAGE));

		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("export.farmPackage"));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);

		return ControllerConstants.Views.Pages.MyAccountPages.MYACCOUNTFARMPACKAGEPAGE;
	}

	@RequestMapping(value = "/orderApproval/" + ORDER_CODE_PATH_VARIABLE_PATTERN, method = RequestMethod.GET)
	@RequireHardLogIn
	public String orderApproval(@PathVariable("orderCode") final String orderCode, final Model model)
			throws CMSItemNotFoundException
	{
		try
		{
			final OrderData orderDetails = orderFacade.getOrderDetailsForCode(orderCode);
			model.addAttribute("orderData", orderDetails);

			final List<Breadcrumb> breadcrumbs = accountBreadcrumbBuilder.getBreadcrumbs(null);
			breadcrumbs.add(new Breadcrumb("/my-account/orders",
					getMessageSource().getMessage("text.account.orderApprovalDashboard", null, getI18nService().getCurrentLocale()),
					null));
			breadcrumbs.add(new Breadcrumb("#", getMessageSource().getMessage("text.account.order.orderBreadcrumb", new Object[]
			{ orderDetails.getCode() }, "Order {0}", getI18nService().getCurrentLocale()), null));
			model.addAttribute(BREADCRUMBS, breadcrumbs);
		}
		catch (final UnknownIdentifierException e)
		{
			LOG.warn("Attempted to load a order that does not exist or is not visible", e);
			return REDIRECT_MY_ACCOUNT;
		}
		storeCmsPageInModel(model, getContentPageForLabelOrId(ORDER_DETAIL_CMS_PAGE));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(ORDER_DETAIL_CMS_PAGE));
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);
		return ControllerConstants.Views.Pages.Account.ACCOUNTORDERAPPROVALDETAILSPAGE;
	}

	/* Bulk Feed Homepage */
	/**
	 *
	 * @param model
	 * @param bulkConfirmOrderProducts
	 * @return BulkProductDetailsPage
	 * @throws CMSItemNotFoundException
	 */
	@RequestMapping(value =
	{ "/new-bulk-feed-order", "/new-bulk-feed-order/{incoterm}" }, method =
	{ RequestMethod.GET })
	@RequireHardLogIn
	public String showProducts(@PathVariable(name = "incoterm", required = false) final String incoterm, final Model model,
			final BulkOrderConfirmationForm bulkConfirmOrderProducts) throws CMSItemNotFoundException
	{


		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");

		if (user.getTradingAccountId() != null && !user.getTradingAccountId().isEmpty())
		{


			model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("bulkfeed.new.order"));
			model.addAttribute("bulkFeedOrderMinQty", Config.getParameter("bulkFeed.minimum.orderQty"));


			storeCmsPageInModel(model, getContentPageForLabelOrId(NEW_BULK_FEED_ORDER));
			setUpMetaDataForContentPage(model, getContentPageForLabelOrId(NEW_BULK_FEED_ORDER));

			//Land to incoterm Selection Page
			if (incoterm == null)
			{
				model.addAttribute("bulkIncotermsMap", AgriCoreConstants.BULK_INCOTERMS);
				return ControllerConstants.Views.Pages.Account.BULKCHOOSEDELIVERY;
			}

			try
			{
				getAllBulkProductsForCategory("bulkFeed", incoterm, null, model);
				final BulkOrderForm bulkOrderForm = sessionService.getAttribute("bulkConfirmOrderProducts");
				if (bulkOrderForm != null && CollectionUtils.isNotEmpty(bulkOrderForm.getBulkProducts()))
				{
					final Map<String, String> bulkProductSelectedMap = bulkOrderForm.getBulkProducts().stream()
							.collect(Collectors.toMap(ProductData::getCode, ProductData::getVolume));
					model.addAttribute("bulkProductSelectedMap", bulkProductSelectedMap);
				}
				model.addAttribute(WebserviceConstants.SAP.EV_BUSINESS_MANAGER, sessionService.getAttribute(WebserviceConstants.SAP.EV_BUSINESS_MANAGER));

			}
			catch (final BusinessException e)
			{
				model.addAttribute(WebserviceConstants.STATUS, WebserviceConstants.SITE_DOWN);
			}
		}

		else
		{
			// For non-trading users,
			return REDIRECT_MY_ACCOUNT;
		}
		return ControllerConstants.Views.Pages.Account.BULKPRODUCTDETAILSPAGE;
	}

	/**
	 *
	 * @param categoryCode
	 * @param model
	 * @return BulkProductOrderForm Page
	 * @throws BusinessException
	 */
	@RequestMapping(value = "/searchBulkProducts/" + BULK_FEED_CATEGORY + "/" + BULK_FEED_INCOTERM, method = RequestMethod.GET)
	@RequireHardLogIn
	public String getAllBulkProductsForCategory(@PathVariable("bulkFeedCategoryCode") final String bulkFeedCategoryCode,
			@PathVariable("incoterm") final String incoterm, @RequestParam(value = "text", required = false) final String text,
			final Model model) throws BusinessException
	{

		List<ProductData> productDataList = null;
		final SearchStateData searchState = new SearchStateData();
		final PageableData pageableData = this.createPageableData(0, getProductsPerPage(), null, ShowMode.All);

		if (StringUtils.isNotBlank(text))
		{
			final String searchText = text.trim().toLowerCase();
			final SearchQueryData searchQueryData = new SearchQueryData();
			searchQueryData.setValue(XSSFilterUtil.filter(searchText.trim()).concat(SearchPageController.PRODUCT_TAG_TEXT_SEARCH));
			searchState.setQuery(searchQueryData);
		}

		final ProductCategorySearchPageData<SearchStateData, ProductData, CategoryData> searchPageData = this.productSearchFacade
				.categorySearch(bulkFeedCategoryCode, searchState, pageableData);
		productDataList = searchPageData.getResults();

		// Fetch best price from SAP.
		if (CollectionUtils.isNotEmpty(productDataList))
		{
			if (checkSAPDowntimeFlag())
			{
				updateBulkOrderPriceFromProductData(productDataList);
			}
			else
			{
				updateBulkProductPriceFromSAPWS(incoterm, model, productDataList);

			}
			// Update selected products on search in order to persist selections between filters
			final BulkOrderForm sessionForm = sessionService.getAttribute("bulkConfirmOrderProducts");
			if (sessionForm != null && CollectionUtils.isNotEmpty(sessionForm.getBulkProducts()))
			{
				final Map<String, String> bulkProductSelectedMap = sessionForm.getBulkProducts().stream()
					.collect(Collectors.toMap(ProductData::getCode, ProductData::getVolume));
				model.addAttribute("bulkProductSelectedMap", bulkProductSelectedMap);
			}
		}
		final BulkOrderForm bulkOrderForm = new BulkOrderForm();
		bulkOrderForm.setIncoTerm(incoterm);
		bulkOrderForm.setBulkProducts(productDataList);
		model.addAttribute("bulkOrderForm", bulkOrderForm);
		model.addAttribute("productData", new ProductData());
		return ControllerConstants.Views.Pages.Account.BULKFEEDORDERFORM;

	}

	/**
	 * @param incoterm
	 * @param model
	 * @param productDataList
	 */
	private void updateBulkProductPriceFromSAPWS(final String incoterm, final Model model, final List<ProductData> productDataList)
	{
		try
		{
			sapPriceCacheFacade.getBestPriceFormSap(productDataList, AgriCoreConstants.BULK_INCOTERMS.get(incoterm));
		}
		catch (final NullPointerException nullexception)
		{
			LOG.debug(nullexception);
			GlobalMessages.addErrorMessage(model, WEBSERVICE_NOT_AVAILABLE_ERROR_MESSAGE);
			model.addAttribute("disableContinueToCheckout", "true");
		}
		catch (final HttpClientErrorException clientException)
		{
			LOG.debug(clientException);
			GlobalMessages.addErrorMessage(model, WEBSERVICE_NOT_AVAILABLE_ERROR_MESSAGE);
			model.addAttribute("disableContinueToCheckout", "true");
		}
		catch (final BusinessException e)
		{
			LOG.debug(e);
			GlobalMessages.addErrorMessage(model, "webservice.error.message");
		}
	}

	/**
	 * @param productDataList
	 */
	private void updateBulkOrderPriceFromProductData(final List<ProductData> productDataList)
	{
		for (final ProductData productData : productDataList)
		{
			if (null != productData.getPrice() && null != productData.getPrice().getValue())
			{
				if (LOG.isDebugEnabled())
				{
					LOG.debug(String.format("The hybris price determined for bulk prouduct %s, is %s ", productData.getCode(),
							productData.getPrice().getValue()));
				}
				productData.getPrice().setSecondaryPrice(Double.valueOf(productData.getPrice().getValue().doubleValue()));
			}
		}
	}

	@RequestMapping(value = "/your-bulk-order-history", method = RequestMethod.GET)
	@RequireHardLogIn
	public String viewBulkOrder(final Model model) throws CMSItemNotFoundException
	{
		final VetCustomerModel user = (VetCustomerModel) sessionService.getAttribute("user");
		if (user.getTradingAccountId() != null && !user.getTradingAccountId().isEmpty())
		{

			final int endYear = DateUtil.getCurrentYear();
			final int month = DateUtil.getCurrentMonth();

			final String to_period = endYear + String.format("%02d", Integer.valueOf(month));
			String from_period = null;
			if (month == 12)
			{
				from_period = endYear + "01";
			}
			else
			{
				from_period = endYear - 1 + String.format("%02d", Integer.valueOf(month + 1));
			}

			final Date endDate = Date.from(ZonedDateTime.now().minusMonths(11).toInstant());
			final Date startDate = Date.from(ZonedDateTime.now().toInstant());
			model.addAttribute("orderMonths", DateUtil.getYearsWithFuture(endDate, startDate));
			final List<BulkOrderData> bulkorders = bulkOrderFacade.getAllBulkOrderedProducts(user, from_period, to_period);
			if (bulkorders == null)
			{
				GlobalMessages.addErrorMessage(model, WEBSERVICE_NOT_AVAILABLE_ERROR_MESSAGE);
				model.addAttribute(WebserviceConstants.STATUS, WebserviceConstants.SITE_DOWN);
			}

			model.addAttribute("bulkorders", bulkorders);
			model.addAttribute("bulkFeedOrderMinQty", Config.getParameter("bulkFeed.minimum.orderQty"));
			final BulkOrderForm bulkOrderForm = new BulkOrderForm();
			model.addAttribute("bulkOrderForm", bulkOrderForm);
			model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("bulkfeed.order.history"));
		}
		else
		{
			return REDIRECT_MY_ACCOUNT;
		}

		return ControllerConstants.Views.Pages.Account.BULKREORDERPRODUCTSPAGE;

	}

	/* Get all bulk Orders from database */
	@RequestMapping(value = "/farm-management-documents", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getFarmManagementDocuments(final Model model, @RequestParam(value = WebserviceConstants.PDF_DERROR, required = false) boolean pdfDError, final HttpServletRequest request)
			throws CMSItemNotFoundException
	{

		try
		{
			ServiceRespMapper serviceRespMapper = null;
			serviceRespMapper = defaultMyAccountFacade.getFarmDocuments(null, null);
			model.addAttribute("zArchLibraryList", serviceRespMapper.getzArchive_Library());
			model.addAttribute("CategoryLibraryMap", serviceRespMapper.getCategoryLibraryMap());
			model.addAttribute("docYears", serviceRespMapper.getHarvestYears());

		}
		catch (final Exception e)
		{
			LOG.error(e);
			model.addAttribute(WebserviceConstants.STATUS, WebserviceConstants.SITE_DOWN);
		}

		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("account.farmmanagement.documents.breadcrumbs"));
		if(StringUtils.isNotEmpty(request.getParameter("currentOptedConsentTempID"))){
			model.addAttribute("currentOptedConsentTempID",request.getParameter("currentOptedConsentTempID"));
		}
		defaultMyAccountFacade.addAcknowledgementAcceptanceConsentDataToModel(model);
		ConsentForm glanbiaConsentForm = new ConsentForm();
		model.addAttribute("glanbiaConsentForm", glanbiaConsentForm);
		model.addAttribute(WebserviceConstants.PDF_DERROR, pdfDError);
		storeCmsPageInModel(model, getContentPageForLabelOrId(FARM_MANAGEMENT_DOCS));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(FARM_MANAGEMENT_DOCS));


		return ControllerConstants.Views.Pages.Account.FARMMANAGEMENTDOCUMENTS;

	}

	@RequestMapping(value = "/farm-management-documents-search", method = RequestMethod.GET, produces =
	{ "application/json" })
	@RequireHardLogIn
	public String getFarmDocuments(final Model model, @RequestParam(value = "year") final String year,
			@RequestParam(value = "docType") final String docType) throws CMSItemNotFoundException
	{

		ServiceRespMapper serviceRespMapper = null;
		defaultMyAccountFacade.addAcknowledgementAcceptanceConsentDataToModel(model);
		ConsentForm glanbiaConsentForm = new ConsentForm();
		model.addAttribute("glanbiaConsentForm", glanbiaConsentForm);
		try
		{
			serviceRespMapper = defaultMyAccountFacade.getFarmDocuments(docType, year);
			model.addAttribute("CategoryLibraryMap", serviceRespMapper.getCategoryLibraryMap());
		}
		catch (final Exception e)
		{
			LOG.error(e);
			model.addAttribute(WebserviceConstants.STATUS, WebserviceConstants.SITE_DOWN);
			return ControllerConstants.Views.Pages.Account.FARMMANAGEMENTDOCUMENTS;
		}
		return ControllerConstants.Views.Pages.Account.FARMMANAGEMENTDOCUMENTSSEARCHFRAGMENT;
	}

	@RequestMapping(value = "/milk-supplier-details", method = RequestMethod.GET)
	@RequireHardLogIn
	public String getMilkAccountDetails(final Model model, @RequestParam(value = WebserviceConstants.PDF_DERROR, required = false) boolean pdfDError) throws Exception {
		final VetCustomerModel user = sessionService.getAttribute("user");
		final String supplierId = user.getMilkSupplierId();
		if (showPaperlessOptIn(user)) {
			model.addAttribute("isPaperlessOptIn", user.getIsPaperlessOptIn());
		}

		ServiceRespMapper serviceRespMapper = null;
		try {
			serviceRespMapper = defaultMyAccountFacade.getFarmDocuments(null, null);
			List<ZArchive_Library> docList = serviceRespMapper.getzArchive_Library();
			for (ZArchive_Library doc : docList) {
				if (doc.getDoc_type_id().equalsIgnoreCase(ZMKITSV001)) {
					model.addAttribute("milkReturnSummary", doc);
					break;
				}
			}
		} catch (final Exception e) {
			LOG.error(e);
			model.addAttribute(WebserviceConstants.STATUS, WebserviceConstants.SITE_DOWN);
		}
		model.addAttribute(AgriCoreConstants.SUPPLIER_ID, supplierId);
		final List<EtOutput> etOutputList = myAccountDashboardFacade.getLatestTestResults(Boolean.TRUE);

		model.addAttribute("resultEts", defaultMyAccountFacade.changeOrder(etOutputList));

		getQualityResultData(model, DateUtil.lastMonth(), DateUtil.currentMonth(), "");//last param is rows blank means show all the available rows
		getQualityAnalysisData(model, "BFAT");
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs("account.milk.supplier.details.breadcrumbs"));
		model.addAttribute(WebserviceConstants.PDF_DERROR, pdfDError);
		storeCmsPageInModel(model, getContentPageForLabelOrId(MILK_SUPPLIER_DETAILS));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(MILK_SUPPLIER_DETAILS));

		return ControllerConstants.Views.Pages.Account.MILKSUPPLIERDETAILSPAGE;
	}


	@GetMapping(value = "/sustainability-action-payment")
	@RequireHardLogIn
	public String getSustainabilityActionPayment(final Model model) throws Exception
	{
		final VetCustomerModel vetCustomerModel = sessionService.getAttribute("user");
		GapActionResult gapActionResult = glanbiaCustomerSurveyFacade.getSurveyQuesDataFromGapBySupplierId(vetCustomerModel.getMilkSupplierId());
		populateGlanbiaCustomerSurveyCMSPage(model);
		model.addAttribute(AboutUsPageController.BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs(ACCOUNT_SUSTAINABILITY_ACTION_PAYMENT_BREADCRUMBS));
		if (Objects.isNull(gapActionResult)) {
			model.addAttribute(WebserviceConstants.STATUS, WebserviceConstants.SITE_DOWN);
			return ControllerConstants.Views.Pages.Account.SUSTAINABILITYACTIONPAYMENTPAGE;
		}
		SurveyData survey = glanbiaCustomerSurveyFacade.getCurrentUserSurveyQuesDataFromGap(gapActionResult);
		populateSurveyYear(model, survey);
		if (survey.isShowProgressBar()) {
			model.addAttribute(SURVEY, glanbiaCustomerSurveyFacade.getCurrentUserActiveSurveyWithActiveQuestions(gapActionResult, Arrays.asList("Y","V")));
			return ControllerConstants.Views.Pages.Account.SUSTAINABILITYACTIONPAYMENTRESULTSPAGE;
		} else {
			final SurveyForm surveyForm = new SurveyForm();
			model.addAttribute(SURVEY, survey);
			model.addAttribute("surveyForm", surveyForm);
			return ControllerConstants.Views.Pages.Account.SUSTAINABILITYACTIONPAYMENTPAGE;
		}
	}
	@GetMapping(value = "/sustainability-action-payment/edit-selection")
	@RequireHardLogIn
	public String editSustainabilityActionPayment(final Model model) throws Exception
	{
		final VetCustomerModel vetCustomerModel = sessionService.getAttribute("user");
		GapActionResult gapActionResult = glanbiaCustomerSurveyFacade.getSurveyQuesDataFromGapBySupplierId(vetCustomerModel.getMilkSupplierId());
		populateGlanbiaCustomerSurveyCMSPage(model);
		model.addAttribute(AboutUsPageController.BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs(ACCOUNT_SUSTAINABILITY_ACTION_PAYMENT_BREADCRUMBS));
		if (Objects.isNull(gapActionResult)) {
			model.addAttribute(WebserviceConstants.STATUS, WebserviceConstants.SITE_DOWN);
			return ControllerConstants.Views.Pages.Account.SUSTAINABILITYACTIONPAYMENTPAGE;
		}
		SurveyData survey = glanbiaCustomerSurveyFacade.getCurrentUserSurveyQuesDataFromGap(gapActionResult);
		final SurveyForm surveyForm = new SurveyForm();
		model.addAttribute(SURVEY, survey);
		model.addAttribute("surveyForm", surveyForm);
		populateSurveyYear(model, survey);
		return ControllerConstants.Views.Pages.Account.SUSTAINABILITYACTIONPAYMENTPAGE;
	}
	@PostMapping(value = "/sustainability-action-payment")
	@RequireHardLogIn
	public String getSustainabilityActionPaymentResult(final Model model, SurveyForm surveyForm) throws Exception
	{
		GapActionResult gapActionResult = glanbiaCustomerSurveyFacade.postSurveyQuesDataToGapBySupplierId(surveyForm.getSurveyQuestionResult());
		populateGlanbiaCustomerSurveyCMSPage(model);
		model.addAttribute(AboutUsPageController.BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs(ACCOUNT_SUSTAINABILITY_ACTION_PAYMENT_BREADCRUMBS));
		if (Objects.isNull(gapActionResult)) {
			model.addAttribute(WebserviceConstants.STATUS, WebserviceConstants.SITE_DOWN);
			return ControllerConstants.Views.Pages.Account.SUSTAINABILITYACTIONPAYMENTPAGE;
		}
		SurveyData survey = glanbiaCustomerSurveyFacade.getCurrentUserActiveSurveyWithActiveQuestions(gapActionResult, Arrays.asList("Y","V"));
		model.addAttribute(SURVEY, survey);
		populateSurveyYear(model, survey);
		return ControllerConstants.Views.Pages.Account.SUSTAINABILITYACTIONPAYMENTRESULTSPAGE;
	}

	private void populateGlanbiaCustomerSurveyCMSPage(Model model) throws CMSItemNotFoundException {
		storeCmsPageInModel(model, getContentPageForLabelOrId(SUSTAINABILITY_ACTION_PAYMENT));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(SUSTAINABILITY_ACTION_PAYMENT));
		storeCmsPageInModel(model, getContentPageForLabelOrId(SUSTAINABILITY_ACTION_PAYMENT));
	}

	  @GetMapping(value = "/collection-comparison-year", produces =
			  { "application/json" })
	  @RequireHardLogIn
	  @ResponseBody
	  public List<EtYearsVolComp> getYearVolCompData(@RequestParam(value = WebserviceConstants.SAP_CPI.PERIOD, defaultValue = "201905")
	  final String period){
		  final VetCustomerModel user = sessionService.getAttribute("user");
		  if (StringUtils.isNotBlank(user.getMilkSupplierId()))
			{
			  final List<EtYearsVolComp> etYearsVolCompResults = defaultMyAccountFacade.getYearVolCompResults(user.getMilkSupplierId(),period);
			  return etYearsVolCompResults;
			}
		  return null;
	  }

	  @GetMapping(value = "/collection-comparison-month", produces =
			  { "application/json" })
	  @ResponseBody
	  @RequireHardLogIn
	  public List<EtMonthsVolComp> getMonthsVolCompData(@RequestParam(value = WebserviceConstants.SAP_CPI.PERIOD, defaultValue = "201905")
	  final String period){
		  final VetCustomerModel user = sessionService.getAttribute("user");
		  if (StringUtils.isNotBlank(user.getMilkSupplierId()))
			{
			  final List<EtMonthsVolComp> etMonthsVolCompResults = defaultMyAccountFacade.getMonthsVolCompResults(user.getMilkSupplierId(),period);
			  return etMonthsVolCompResults;
			}
		  return null;
	  }

	@GetMapping("/notificationcentre")
	@RequireHardLogIn
	public String notificationCentre(final Model model) throws CMSItemNotFoundException{

		ContentPageModel page = getContentPageForLabelOrId(NOTIFICATION_CENTRE);
		storeCmsPageInModel(model, page);
		setUpMetaDataForContentPage(model, page);
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs(TEXT_NOTIFICATION_CENTRE));

		return ControllerConstants.Views.Pages.Account.ACCOUNTNOTIFICATIONCENTREPAGE;
	}

	@GetMapping("/myaccountfaq")
	@RequireHardLogIn
	public String myAccountFaq(final Model model) throws CMSItemNotFoundException{

		ContentPageModel page = getContentPageForLabelOrId(MY_ACCOUNT_FAQ);
		storeCmsPageInModel(model, page);
		setUpMetaDataForContentPage(model, page);
		model.addAttribute(BREADCRUMBS, accountBreadcrumbBuilder.getBreadcrumbs(TEXT_MY_ACCOUNT_FAQ));

		return ControllerConstants.Views.Pages.Account.ACCOUNTGUIDEPAGE;
	}

	public void getQualityResultData(final Model model, String rows)
	{
		final String startYearM = MilkCalanderUtil.getMilkCalendarStartYEAR_MONTH();
		final String endYearM = MilkCalanderUtil.getMilkCalendarEndYEAR_MONTH();

		final String from = milkCalendarDateFormatter.format(DateUtil.getDate(startYearM, "yyyyMM"));
		final String to = milkCalendarDateFormatter.format(DateUtil.getDate(endYearM, "yyyyMM"));

		getQualityResultData(model, from, to, rows);
	}

	public void getQualityResultData(final Model model, String from, String to, String rows)
	{
		ServiceRespMapper serviceRespMapper = null;
		try
		{
			serviceRespMapper = defaultMyAccountFacade.getSupplierResults(from, to, rows);

			if (serviceRespMapper != null)
			{
				if (serviceRespMapper.getHttpStatus().value() == 200 && serviceRespMapper.getResultsMap().isEmpty())
				{

					final Map<Date, MilkResults> responseMap = new HashMap<Date, MilkResults>();
					final MilkResults mkResult = new MilkResults();
					mkResult.setPeriodString("nodata");
					responseMap.put(new Date(), mkResult);
					serviceRespMapper.setResultsMap(responseMap);

				}

				model.addAttribute("resultJson", gson.toJson(serviceRespMapper.getResultsMap()));

			}
		}
		catch (final Exception e)
		{
			LOG.error(" Error on AccountPageController.account() method and error is + " + e, e);
		}
	}
	private void getQualityAnalysisData(final Model model, final String type)
	{
		ServiceRespMapper serviceRespMapper = null;
		final String sYear = DateUtil.getStringDate(new Date(), DateUtil.yyyyMM);
		try
		{
			serviceRespMapper = defaultMyAccountFacade.getQualityAnalysis(sYear, type, false);

			final Map<String, String> map = new HashMap<>();
			for (final String key : WebserviceConstants.TEST_TYPES)
			{
				map.put(key, getValue(key));
			}

			final Map<String, String> metricMap = new HashMap<>();

			for (final String key : WebserviceConstants.TEST_TYPES)
			{
				metricMap.put(key, getValue(key + ".unit"));
			}

			model.addAttribute("typeMap", map);
			model.addAttribute("metricMap", metricMap);

			if (serviceRespMapper != null)
			{
				model.addAttribute("resultJsonforQualityAnalysis", gson.toJson(serviceRespMapper.getResponseMap()));
				model.addAttribute("result", serviceRespMapper.getResponseMap());
			}

			model.addAttribute("analysisType", type);
			model.addAttribute("years", DateUtil.getPastYears());
			model.addAttribute("months", DateUtil.getMonthList());
			model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));

		}
		catch (final Exception e)
		{
			LOG.error(" Error on AccountPageController.getQualityAnalysis() method and error is + " + e);
		}
	}

	private void setUpAccountAddressEditPage(final Model model) throws CMSItemNotFoundException
	{
		storeCmsPageInModel(model, getContentPageForLabelOrId(ADD_EDIT_ADDRESS_CMS_PAGE));
		setUpMetaDataForContentPage(model, getContentPageForLabelOrId(ADD_EDIT_ADDRESS_CMS_PAGE));

		final List<Breadcrumb> breadcrumbs = accountBreadcrumbBuilder.getBreadcrumbs(null);
		breadcrumbs.add(new Breadcrumb(ADDRESS_BOOK,
				getMessageSource().getMessage(TEXT_ACCOUNT_ADDRESSBOOK, null, getI18nService().getCurrentLocale()), null));
		breadcrumbs.add(new Breadcrumb("#",
				getMessageSource().getMessage("text.account.addressBook.addEditAddress", null, getI18nService().getCurrentLocale()),
				null));
		model.addAttribute(BREADCRUMBS, breadcrumbs);
		model.addAttribute(ThirdPartyConstants.META_ROBOTS, ThirdPartyConstants.NOINDEX_NOFOLLOW);

		final VetCustomerModel user = sessionService.getAttribute("user");
		model.addAttribute(CUSTOMER_TYPE, sessionService.getAttribute(CustomerType));
	}

	@RequestMapping(value = "/reorder", method =
			{ RequestMethod.PUT, RequestMethod.POST }) // NOSONAR
	@RequireHardLogIn
	public String reorder(@RequestParam(value = "orderCode") final String orderCode, final RedirectAttributes redirectModel)
			throws CMSItemNotFoundException, InvalidCartException, ParseException, CommerceCartModificationException
	{
		try
		{
			// create a cart from the order and set it as session cart.
			b2bCheckoutFacade.createCartFromOrder(orderCode);
			// validate for stock and availability
			final List<CartModificationData> cartModifications = cartFacade.validateCartData();
			for (final CartModificationData cartModification : cartModifications)
			{
				if (CommerceCartModificationStatus.NO_STOCK.equals(cartModification.getStatusCode()))
				{
					GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
							"basket.page.message.update.reducedNumberOfItemsAdded.noStock", new Object[]
									{ XSSFilterUtil.filter(cartModification.getEntry().getProduct().getName()) });
					break;
				}
				else if (cartModification.getQuantity() != cartModification.getQuantityAdded())
				{
					// item has been modified to match available stock levels
					GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
							"basket.information.quantity.adjusted");
					break;
				}
				// TODO: handle more specific messaging, i.e. out of stock, product not available
			}
		}
		catch (final IllegalArgumentException e)
		{
			LOG.error(String.format("Unable to reorder %s", orderCode), e);
			GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, "order.reorder.error", null);
			return REDIRECT_ORDER_LIST_URL;
		}
		return REDIRECT_PREFIX + "/cart";
	}

	@PostMapping(path = "/consents/give/{consentTemplateId}/{version}")
	@de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn
	public String giveConsent(@PathVariable final String consentTemplateId, @PathVariable final Integer version,
							  final RedirectAttributes redirectModel) {
		try {
			if(getSiteConfigService().getProperty(MARKETING_CONSENT).equals(consentTemplateId)){
				defaultMyAccountFacade.updateUserConsent(Boolean.TRUE);
				triggerEmarsysContactCUEvent(WebserviceConstants.Emarsys.CONSENT_UPDATE, getVetCustomerData());
			} else {
				getConsentFacade().giveConsent(consentTemplateId, version);
			}
			GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER, TEXT_ACCOUNT_CONSENT_GIVEN);
		} catch (final ModelNotFoundException | AmbiguousIdentifierException e) {
			LOG.warn(String.format("ConsentTemplate with code [%s] and version [%s] was not found", consentTemplateId, version), e);
			GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
					TEXT_ACCOUNT_CONSENT_TEMPLATE_NOT_FOUND, null);
		} catch (final CommerceConsentGivenException e) {
			LOG.warn(String.format("ConsentTemplate with code [%s] and version [%s] already has a given consent", consentTemplateId,
					version), e);
			GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, TEXT_ACCOUNT_CONSENT_ALREADY_GIVEN,
					null);
		}
		customerConsentDataStrategy.populateCustomerConsentDataInSession();
		return REDIRECT_TO_PROFILE_PAGE;
	}

	@PostMapping(path = "/consents/withdraw/{consentCode}")
	@de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn
	public String withdrawConsent(@PathVariable final String consentCode, final RedirectAttributes redirectModel)
			throws CMSItemNotFoundException {
		try {
			if(getSiteConfigService().getProperty(MARKETING_CONSENT).equals(consentCode)){
				defaultMyAccountFacade.updateUserConsent(Boolean.FALSE);
				triggerEmarsysContactCUEvent(WebserviceConstants.Emarsys.CONSENT_UPDATE, getVetCustomerData());
			} else {
				getConsentFacade().withdrawConsent(consentCode);
			}
			GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER, TEXT_ACCOUNT_CONSENT_WITHDRAWN);
		} catch (final ModelNotFoundException e) {
			LOG.warn(String.format("Consent with code [%s] was not found", consentCode), e);
			GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER, TEXT_ACCOUNT_CONSENT_NOT_FOUND,
					null);
		} catch (final CommerceConsentWithdrawnException e) {
			LOG.error(String.format("Consent with code [%s] is already withdrawn", consentCode), e);
			GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.ERROR_MESSAGES_HOLDER,
					TEXT_ACCOUNT_CONSENT_ALREADY_WITHDRAWN, null);
		}
		customerConsentDataStrategy.populateCustomerConsentDataInSession();
		return REDIRECT_TO_PROFILE_PAGE;
	}

	@GetMapping(path = "/consent")
	@de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn
	public ResponseEntity getConsentDataFromEmarsys() {
		ResponseEntity<String> response = null;
		response = emarsysContactDataService.getData(getVetCustomerData(), WebserviceConstants.Emarsys.FETCH_DATA);
		return response;
	}

	/**
	 *
	 * @param user
	 * @return boolean - True if the user not in USERGROUP_EMP || ONLINE_SHOPPER_UG User Groups
	 */
	private boolean showPaperlessOptIn(VetCustomerModel user) {
		final String userGroup = myAccountDashboardFacade.getUerGroup(user).getUid();
		return (StringUtils.isNotEmpty(user.getMilkSupplierId()) || StringUtils.isNotEmpty(user.getMilkVendorId()) || StringUtils.isNotEmpty(user.getTradingAccountId())) && !(AgriCoreConstants.USERGROUP_EMP.equals(userGroup) || AgriCoreConstants.ONLINE_SHOPPER_UG.equals(userGroup));
	}

	/**
	 * @param model
	 * @param survey
	 */
	private void populateSurveyYear(final Model model, final SurveyData survey)
	{
		final String surveyCode = survey.getCode();
		if(StringUtils.isNotBlank(surveyCode))
		{
			final int lastIndex = survey.getCode().lastIndexOf("_");
			final String extractedYearValue = surveyCode.substring(lastIndex + 1);
			model.addAttribute("yearValueOfSurvey", extractedYearValue);
		}
	}

	@ModelAttribute("isMobileDevice")
	public boolean isMobileDevice()
	{
		final DeviceData deviceData = defaultDeviceDetectionFacade.getCurrentDetectedDevice();
		if (deviceData != null) {
			return deviceData.getMobileBrowser().booleanValue();
		}
		return false;
	}
}