package io.tolgee.ee.component

import io.tolgee.model.Language
import io.tolgee.model.UserAccount
import io.tolgee.model.views.TaskPerUserReportView
import io.tolgee.model.views.TaskWithScopeView
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class TaskReportHelper(
  private val task: TaskWithScopeView,
  private val report: List<TaskPerUserReportView>,
) {
  fun formatDate(date: Date): String {
    return DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.ENGLISH).format(date)
  }

  fun formatLanguage(language: Language?): String {
    if (language == null) {
      return ""
    }
    val result = StringBuilder(language.name)
    if (language.name != language.tag) {
      result.append(" (${language.tag})")
    }
    return result.toString()
  }

  fun formatUserName(user: UserAccount): String {
    val result = StringBuilder(user.name)
    if (user.name != user.username) {
      result.append(" (${user.username})")
    }
    return result.toString()
  }

  fun capitalize(text: String): String {
    return text.lowercase().replaceFirstChar {
      if (it.isLowerCase()) {
        it.titlecase(
          Locale.getDefault(),
        )
      } else {
        it.toString()
      }
    }
  }

  fun generateExcelReport(): XSSFWorkbook {
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("Task Report")

    sheet.createRow(0).let {
      it.createCell(0).setCellValue("Task name")
      it.createCell(1).setCellValue(task.name)
    }

    sheet.createRow(1).let {
      it.createCell(0).setCellValue("Task description")
      it.createCell(1).setCellValue(task.description)
    }

    sheet.createRow(2).let {
      it.createCell(0).setCellValue("Type")
      it.createCell(1).setCellValue(capitalize(task.type.toString()))
    }

    sheet.createRow(3).let {
      it.createCell(0).setCellValue("Project name")
      it.createCell(1).setCellValue(task.project.name)
    }

    sheet.createRow(4).let {
      it.createCell(0).setCellValue("Base language")
      it.createCell(1).setCellValue(formatLanguage(task.project.baseLanguage))
    }

    sheet.createRow(5).let {
      it.createCell(0).setCellValue("Target language")
      it.createCell(1).setCellValue(formatLanguage(task.language))
    }

    sheet.createRow(6).let {
      it.createCell(0).setCellValue("Created at")
      task.createdAt?.let { createdAt ->
        it.createCell(1).setCellValue(formatDate(createdAt))
      }
    }

    sheet.createRow(7).let {
      it.createCell(0).setCellValue("Created by")
      it.createCell(1).setCellValue(formatUserName(task.author))
    }

    sheet.createRow(8).let {
      it.createCell(0).setCellValue("Due date")
      it.createCell(1).setCellValue(task.dueDate?.let { formatDate(it) })
    }

    sheet.createRow(10).let {
      it.createCell(1).setCellValue("Keys")
      it.createCell(2).setCellValue("Words")
      it.createCell(3).setCellValue("Characters")
    }

    val dataRow = sheet.createRow(11)
    dataRow.createCell(0).setCellValue("Total to translate")
    dataRow.createCell(1).setCellValue(task.totalItems.toDouble())
    dataRow.createCell(2).setCellValue(task.baseWordCount.toDouble())
    dataRow.createCell(3).setCellValue(task.baseCharacterCount.toDouble())

    report.forEachIndexed { index, taskReport ->
      val row = sheet.createRow(12 + index)
      row.createCell(0).setCellValue(formatUserName(taskReport.user))
      row.createCell(1).setCellValue(taskReport.doneItems.toDouble())
      row.createCell(2).setCellValue(taskReport.baseWordCount.toDouble())
      row.createCell(3).setCellValue(taskReport.baseCharacterCount.toDouble())
    }

    sheet.setColumnWidth(0, 8000)
    sheet.setColumnWidth(1, 4000)

    return workbook
  }
}
