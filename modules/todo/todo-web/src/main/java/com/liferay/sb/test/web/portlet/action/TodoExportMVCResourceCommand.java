// 
//  //
/**
*  Copyright (C) 2020 Yasuyuki Takeo All rights reserved.
*
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU Lesser General Public License as published by
*  the Free Software Foundation, either version 3 of the License, or
*  (at your option) any later version.
*
*  This program is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
*  GNU Lesser General Public License for more details.
*/
//  //
package com.liferay.sb.test.web.portlet.action;

import com.liferay.portal.kernel.dao.search.DisplayTerms;
import com.liferay.portal.kernel.dao.search.SearchContainer;
import com.liferay.portal.kernel.dao.search.SearchContainerResults;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.sb.test.constants.TodoPortletKeys;
import com.liferay.sb.test.model.Todo;
import com.liferay.sb.test.web.util.TodoViewHelper;

import java.io.IOException;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.LinkedList;
import java.util.List;

import javax.portlet.PortletPreferences;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Export Resource Command
 *
 * @author Softbless
 */
@Component(
	immediate = true,
	property = {
		"javax.portlet.name=" + TodoPortletKeys.TODO,
		"javax.portlet.name=" + TodoPortletKeys.TODO_ADMIN,
		"mvc.command.name=/todo/export"
	},
	service = MVCResourceCommand.class
)
public class TodoExportMVCResourceCommand implements MVCResourceCommand {

	@Override
	public boolean serveResource(
		ResourceRequest resourceRequest, ResourceResponse resourceResponse) {

		String cmd = ParamUtil.getString(resourceRequest, Constants.CMD, "");
		
//  //
		if (!cmd.equals(Constants.EXPORT)) {
			return false;
		}

		TodoConfiguration todoConfiguration =
			(TodoConfiguration)resourceRequest.getAttribute(
				TodoConfiguration.class.getName());

		PortletPreferences portletPreferences =
			resourceRequest.getPreferences();

		String dateFormatVal = HtmlUtil.escape(
			portletPreferences.getValue(
				"dateFormat",
				Validator.isNull(todoConfiguration) ? "yyyy/MM/dd" :
				todoConfiguration.dateFormat()));

		SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatVal);

		String keywords = ParamUtil.getString(
			resourceRequest, DisplayTerms.KEYWORDS);
		String orderByCol = ParamUtil.getString(
			resourceRequest, SearchContainer.DEFAULT_ORDER_BY_COL_PARAM,
			"todoId");
		String orderByType = ParamUtil.getString(
			resourceRequest, SearchContainer.DEFAULT_ORDER_BY_TYPE_PARAM,
			"asc");

		String filename = Constants.EXPORT;

		SearchContainerResults<Todo> searchContainerResults = null;

		try {
			if (Validator.isNull(keywords)) {
				searchContainerResults = _todoViewHelper.getListFromDB(
					resourceRequest, -1, -1, orderByCol, orderByType,
					new int[] {WorkflowConstants.STATUS_APPROVED});
			}
			else {
				searchContainerResults = _todoViewHelper.getListFromIndex(
					resourceRequest, -1, -1, WorkflowConstants.STATUS_APPROVED);
			}
		}
		catch (ParseException | SearchException e) {
			e.printStackTrace();
		}

		try (Workbook workbook = new HSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("data");

			List<String> headers = new LinkedList<>();

            headers.add("TodoId");
            headers.add("Title");
            headers.add("TodoBooleanStat");
            headers.add("TodoDateTime");
            headers.add("TodoDocumentLibrary");
            headers.add("TodoDouble");
            headers.add("TodoInteger");
            headers.add("TodoRichText");
            headers.add("TodoText");

			// Header

			Row headerRow = sheet.createRow(0);

			for (int i = 0; i < headers.size(); i++) {
				Cell cell = headerRow.createCell(i);

				cell.setCellValue(headers.get(i));
			}

			if (Validator.isNotNull(searchContainerResults) &&
				(searchContainerResults.getTotal() > 0)) {

				List<Todo> datas = searchContainerResults.getResults();

				for (int i = 0; i < searchContainerResults.getTotal(); i++) {
					Row row = sheet.createRow(i + 1);

					for (int j = 0; j < headers.size(); j++) {
						Cell cell = row.createCell(j);
						switch (j) {
			            case 0:
								cell.setCellValue(datas.get(i).getTodoId());

								break;
			            case 1:
								cell.setCellValue(datas.get(i).getTitle());

								break;
			            case 2:
								cell.setCellValue(datas.get(i).getTodoBooleanStat());

								break;
			            case 3:
								cell.setCellValue(dateFormat.format(datas.get(i).getTodoDateTime()));

								break;
			            case 4:
								cell.setCellValue(datas.get(i).getTodoDocumentLibrary());

								break;
			            case 5:
								cell.setCellValue(datas.get(i).getTodoDouble());

								break;
			            case 6:
								cell.setCellValue(datas.get(i).getTodoInteger());

								break;
			            case 7:
								cell.setCellValue(datas.get(i).getTodoRichText());

								break;
			            case 8:
								cell.setCellValue(datas.get(i).getTodoText());

								break;
							default:

								break;
						}
					}
				}
			}

			resourceResponse.setContentType(
				ContentTypes.APPLICATION_VND_MS_EXCEL);
			resourceResponse.setProperty(
				"Content-Disposition",
				"attachment; filename=\"" + filename + ".xls\"");
			workbook.write(resourceResponse.getPortletOutputStream());

			return true;
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
// //
		
		return false;
	}

	@Reference(unbind = "-")
	public void setViewHelper(TodoViewHelper todoViewHelper) {
		_todoViewHelper = todoViewHelper;
	}

	private TodoViewHelper _todoViewHelper;

}