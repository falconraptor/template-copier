import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.{File, FilenameFilter}

import scala.swing._
import scala.swing.event.SelectionChanged

object GUI extends SimpleSwingApplication {
	var tabSelected: String = Main.config.getString("tab")

	override def top: Frame = new MainFrame {
		title = "Template Copier"
		contents = new Panel {
			val tabs = new TabbedPane
			for (p <- Main.paths) {
				val files: Seq[String] = try {
					new File(p._2).list(new FilenameFilter {
						override def accept(dir: File, name: String): Boolean = name.endsWith(".txt")
					}).toSeq
				} catch {
					case e: Exception => println(p._1 + " => " + p._2 + " = " + e); Seq.empty[String]
				}
				val list: ListView[String] = new ListView[String](files) {
					selection.intervalMode = ListView.IntervalMode.Single
				}
				listenTo(list.selection)
				reactions += {
					case SelectionChanged(`list`) =>
						val index: Int = list.selection.leadIndex
						val item: String = list.listData(index)
						val clipboard = Toolkit.getDefaultToolkit.getSystemClipboard
						val sel = new StringSelection(new Read(p._2 + "/" + item).read.mkString("\n"))
						clipboard.setContents(sel, sel)
				}
				val scroll: ScrollPane = new ScrollPane(list)
				tabs.pages += new TabbedPane.Page(p._1, scroll, "Templates from " + p._2)
			}
			listenTo(tabs.selection)
			reactions += {
				case SelectionChanged(`tabs`) =>
					tabSelected = tabs.selection.page.title
			}
			if (tabSelected != "") tabs.selection.page = tabs.pages.find(_.title == tabSelected).head
			else if (tabSelected == "") tabSelected = tabs.selection.page.title
			_contents += tabs
		}

		override def closeOperation(): Unit = quit()
	}

	override def shutdown() {
		Main.updateConfig("tab", tabSelected, "Selected tab")
		Main.saveConfig()
	}
}
