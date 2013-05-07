/*
 * Copyright (c) 2002-2013, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.document.web;

import fr.paris.lutece.plugins.document.business.Document;
import fr.paris.lutece.plugins.document.business.DocumentHome;
import fr.paris.lutece.plugins.document.business.DocumentType;
import fr.paris.lutece.plugins.document.business.DocumentTypeHome;
import fr.paris.lutece.plugins.document.utils.IntegerUtils;
import fr.paris.lutece.portal.service.html.XmlTransformerService;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.mail.MailService;
import fr.paris.lutece.portal.service.message.AdminMessage;
import fr.paris.lutece.portal.service.message.AdminMessageService;
import fr.paris.lutece.portal.service.template.AppTemplateService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.portal.web.constants.Messages;
import fr.paris.lutece.util.date.DateUtil;
import fr.paris.lutece.util.html.HtmlTemplate;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;


/**
 * Document Content JspBean
 */
public class DocumentContentJspBean
{
    //////////////////////////////////////////////////////////////////////////////
    // Constants
    private static final String XSL_UNIQUE_PREFIX = "document-";

    // Parameters
    private static final String PARAMETER_DOCUMENT_ID = "document_id";
    private static final String PARAMETER_SEND_MESSAGE = "send";
    private static final String PARAMETER_VISITOR_LASTNAME = "visitor_last_name";
    private static final String PARAMETER_VISITOR_FIRSTNAME = "visitor_first_name";
    private static final String PARAMETER_VISITOR_EMAIL = "visitor_email";
    private static final String PARAMETER_RECIPIENT_EMAIL = "recipient_email";
    private static final String PARAMETER_COMMENT_DOCUMENT = "comment";

    // Markers
    private static final String MARK_DOCUMENT = "document";
    private static final String MARK_BASE_URL = "base_url";
    private static final String MARK_PREVIEW = "preview";
    private static final String MARK_PORTAL_DOMAIN = "portal_domain";
    private static final String MARK_PORTAL_NAME = "portal_name";
    private static final String MARK_PORTAL_URL = "portal_url";
    private static final String MARK_ALERT = "alert";
    private static final String MARK_VISITOR_LAST_NAME = "visitor_last_name";
    private static final String MARK_VISITOR_FIRST_NAME = "visitor_first_name";
    private static final String MARK_VISITOR_EMAIL = "visitor_email";
    private static final String MARK_RECIPIENT_EMAIL = "recipient_email";
    private static final String MARK_COMMENT_DOCUMENT = "comment";
    private static final String MARK_CURRENT_DATE = "current_date";

    // Properties
    private static final String PROPERTY_SENDING_OK = "document.message.sending.ok";
    private static final String PROPERTY_SENDING_NOK = "document.message.sending.nok";
    private static final String PROPERTY_MANDATORY_FIELD = "document.message.mandatory.field";
    private static final String PROPERTY_PORTAL_NAME = "lutece.name";
    private static final String PROPERTY_PORTAL_URL = "lutece.prod.url";

    // Templates
    private static final String TEMPLATE_PAGE_PRINT_DOCUMENT = "skin/plugins/document/page_print_document.html";
    private static final String TEMPLATE_PAGE_SEND_DOCUMENT = "skin/plugins/document/page_send_document.html";
    private static final String TEMPLATE_MESSAGE_DOCUMENT = "skin/plugins/document/message_document.html";

    // Jsp
    private static final String PAGE_SEND_DOCUMENT = "SendDocument.jsp";

    /////////////////////////////////////////////////////////////////////////////
    // page management of printing document

    /**
     * Return the view of an document before printing
     * @param request  Http request
     * @return the HTML page
     */
    public String getPrintDocumentPage( HttpServletRequest request )
    {
        String strDocumentId = request.getParameter( PARAMETER_DOCUMENT_ID );
        int nDocumentId = IntegerUtils.convert( strDocumentId );
        Document document = DocumentHome.findByPrimaryKeyWithoutBinaries( nDocumentId );

        if ( document == null )
        {
            return StringUtils.EMPTY;
        }

        DocumentType type = DocumentTypeHome.findByPrimaryKey( document.getCodeDocumentType(  ) );

        XmlTransformerService xmlTransformerService = new XmlTransformerService(  );

        String strPreview = xmlTransformerService.transformBySourceWithXslCache( document.getXmlValidatedContent(  ),
                type.getContentServiceXslSource(  ), XSL_UNIQUE_PREFIX + type.getContentServiceStyleSheetId(  ), null,
                null );

        Map<String, Object> model = new HashMap<String, Object>(  );

        model.put( MARK_BASE_URL, AppPathService.getBaseUrl( request ) );
        model.put( MARK_PREVIEW, strPreview );
        model.put( MARK_PORTAL_DOMAIN, request.getServerName(  ) );

        HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_PAGE_PRINT_DOCUMENT, request.getLocale(  ),
                model );

        return template.getHtml(  );
    }

    ////////////////////////////////////////////////////////////////////////////
    // Document sending

    /**
     * Return the view of an document before sending
     * @param request  Http request
     * @return the HTML page
     */
    public String getSendDocumentPage( HttpServletRequest request )
    {
        String strDocumentId = request.getParameter( PARAMETER_DOCUMENT_ID );
        int nDocumentId = IntegerUtils.convert( strDocumentId );

        Document document = DocumentHome.findByPrimaryKeyWithoutBinaries( nDocumentId );

        if ( document == null )
        {
            return StringUtils.EMPTY;
        }

        DocumentType type = DocumentTypeHome.findByPrimaryKey( document.getCodeDocumentType(  ) );
        Map<String, Object> model = new HashMap<String, Object>(  );

        XmlTransformerService xmlTransformerService = new XmlTransformerService(  );
        String strPreview = xmlTransformerService.transformBySourceWithXslCache( document.getXmlValidatedContent(  ),
                type.getContentServiceXslSource(  ), XSL_UNIQUE_PREFIX + type.getContentServiceStyleSheetId(  ), null,
                null );

        String strSendMessage = request.getParameter( PARAMETER_SEND_MESSAGE );
        String strAlert = StringUtils.EMPTY;
        String strVisitorLastName = request.getParameter( PARAMETER_VISITOR_LASTNAME );
        strVisitorLastName = ( strVisitorLastName != null ) ? strVisitorLastName : StringUtils.EMPTY;

        String strVisitorFirstName = request.getParameter( PARAMETER_VISITOR_FIRSTNAME );
        strVisitorFirstName = ( strVisitorFirstName != null ) ? strVisitorFirstName : StringUtils.EMPTY;

        String strVisitorEmail = request.getParameter( PARAMETER_VISITOR_EMAIL );
        strVisitorEmail = ( strVisitorEmail != null ) ? strVisitorEmail : StringUtils.EMPTY;

        String strRecipientEmail = request.getParameter( PARAMETER_RECIPIENT_EMAIL );
        strRecipientEmail = ( strRecipientEmail != null ) ? strRecipientEmail : StringUtils.EMPTY;

        String strCommentDocument = request.getParameter( PARAMETER_COMMENT_DOCUMENT );
        strCommentDocument = ( strCommentDocument != null ) ? strCommentDocument : StringUtils.EMPTY;

        if ( ( strSendMessage != null ) && ( strSendMessage.equals( "done" ) ) )
        {
            strAlert = I18nService.getLocalizedString( PROPERTY_SENDING_OK, request.getLocale(  ) );
        }

        if ( ( strSendMessage != null ) && ( strSendMessage.equals( "empty_field" ) ) )
        {
            strAlert = I18nService.getLocalizedString( PROPERTY_MANDATORY_FIELD, request.getLocale(  ) );
        }

        if ( ( strSendMessage != null ) && ( strSendMessage.equals( "error_exception" ) ) )
        {
            strAlert = I18nService.getLocalizedString( PROPERTY_SENDING_NOK, request.getLocale(  ) );
        }

        model.put( MARK_BASE_URL, AppPathService.getBaseUrl( request ) );
        model.put( MARK_PREVIEW, strPreview );
        model.put( MARK_DOCUMENT, document );
        model.put( MARK_PORTAL_DOMAIN, request.getServerName(  ) );
        model.put( MARK_ALERT, strAlert );
        model.put( MARK_VISITOR_LAST_NAME, strVisitorLastName );
        model.put( MARK_VISITOR_FIRST_NAME, strVisitorFirstName );
        model.put( MARK_VISITOR_EMAIL, strVisitorEmail );
        model.put( MARK_RECIPIENT_EMAIL, strRecipientEmail );
        model.put( MARK_COMMENT_DOCUMENT, strCommentDocument );

        HtmlTemplate t = AppTemplateService.getTemplate( TEMPLATE_PAGE_SEND_DOCUMENT, request.getLocale(  ), model );

        return t.getHtml(  );
    }

    /**
     * Processes of document sending
     * @param request The Http request
     * @return The jsp url of the parent page
     */
    public String doSendDocument( HttpServletRequest request )
    {
        String strPortalName = AppPropertiesService.getProperty( PROPERTY_PORTAL_NAME );
        String strPortalUrl = AppPropertiesService.getProperty( PROPERTY_PORTAL_URL );

        String strDocumentId = request.getParameter( PARAMETER_DOCUMENT_ID );
        int nDocumentId = IntegerUtils.convert( strDocumentId );

        Map<String, Object> model = new HashMap<String, Object>(  );
        Document document = DocumentHome.findByPrimaryKeyWithoutBinaries( nDocumentId );

        if ( document == null )
        {
            return AdminMessageService.getMessageUrl( request, Messages.MANDATORY_FIELDS, AdminMessage.TYPE_ERROR );
        }

        DocumentType type = DocumentTypeHome.findByPrimaryKey( document.getCodeDocumentType(  ) );

        XmlTransformerService xmlTransformerService = new XmlTransformerService(  );
        String strPreview = xmlTransformerService.transformBySourceWithXslCache( document.getXmlValidatedContent(  ),
                type.getContentServiceXslSource(  ), XSL_UNIQUE_PREFIX + type.getContentServiceStyleSheetId(  ), null,
                null );

        String strVisitorLastName = request.getParameter( PARAMETER_VISITOR_LASTNAME );
        String strVisitorFirstName = request.getParameter( PARAMETER_VISITOR_FIRSTNAME );
        String strVisitorEmail = request.getParameter( PARAMETER_VISITOR_EMAIL );
        String strRecipientEmail = request.getParameter( PARAMETER_RECIPIENT_EMAIL );

        /*String strCommentDocument = request.getParameter( PARAMETER_COMMENT_DOCUMENT );
        strCommentDocument = ( strCommentDocument != null ) ? strCommentDocument : "";*/
        String strCurrentDate = DateUtil.getCurrentDateString( request.getLocale(  ) );

        // Mandatory Fields
        if ( StringUtils.isBlank( strVisitorLastName ) || StringUtils.isBlank( strVisitorFirstName ) ||
                StringUtils.isBlank( strVisitorEmail ) || StringUtils.isBlank( strRecipientEmail ) )
        {
            return PAGE_SEND_DOCUMENT + "?send=empty_field&" + PARAMETER_DOCUMENT_ID + "=" + nDocumentId +
            "&visitor_last_name=" + strVisitorLastName + "&visitor_first_name=" + strVisitorFirstName +
            "&visitor_email=" + strVisitorEmail + "&recipient_email=" + strRecipientEmail; /* + "&comment=" +
            strCommentDocument;*/
        }

        model.put( MARK_BASE_URL, AppPathService.getBaseUrl( request ) );
        model.put( MARK_PREVIEW, strPreview );
        model.put( MARK_DOCUMENT, document );
        model.put( MARK_PORTAL_DOMAIN, request.getServerName(  ) );
        model.put( MARK_PORTAL_NAME, strPortalName );
        model.put( MARK_PORTAL_URL, strPortalUrl );
        model.put( MARK_CURRENT_DATE, strCurrentDate );
        model.put( MARK_VISITOR_LAST_NAME, strVisitorLastName );
        model.put( MARK_VISITOR_FIRST_NAME, strVisitorFirstName );
        model.put( MARK_VISITOR_EMAIL, strVisitorEmail );
        model.put( MARK_RECIPIENT_EMAIL, strRecipientEmail );

        //     model.put( MARK_COMMENT_DOCUMENT, strCommentDocument );
        HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_MESSAGE_DOCUMENT, request.getLocale(  ), model );

        String strMessageText = template.getHtml(  );
        MailService.sendMailHtml( strRecipientEmail, strVisitorLastName, strVisitorEmail, document.getTitle(  ),
            strMessageText );

        return PAGE_SEND_DOCUMENT + "?send=done&" + PARAMETER_DOCUMENT_ID + "=" + nDocumentId;
    }
}
