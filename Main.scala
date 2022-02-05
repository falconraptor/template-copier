import java.io.{File, FileOutputStream}
import java.nio.channels.{Channels, ReadableByteChannel}
import java.util
import java.util.Map.Entry

import com.typesafe.config._

import scala.swing.FileChooser

object Main {
	val userDir: String = System.getProperty("user.home") + File.separator
	var paths: Seq[(String, String)] = Seq.empty[(String, String)]
	var config: Config = _

	def main(args: Array[String]): Unit = {
		if (!new File(userDir + "templateCopier.conf").exists()) {
			val source: ReadableByteChannel = Channels.newChannel(this.getClass.getClassLoader.getResourceAsStream("reference.conf"))
			val dest: FileOutputStream = new FileOutputStream(new File(userDir + "templateCopier.conf"))
			dest.getChannel.transferFrom(source, 0, Long.MaxValue)
			source.close()
			dest.close()
		}
		config = ConfigFactory.parseFileAnySyntax(new File(userDir + "templateCopier.conf"), ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF).setOriginDescription("").setAllowMissing(false))
		val tab = try {
			config.getString("tab")
		} catch {
			case e: Exception => ""
		}
		if (tab == "") {
			val chooser: FileChooser = new FileChooser(new File("."))
			chooser.title = "Path to user defined templates"
			chooser.fileSelectionMode = FileChooser.SelectionMode.DirectoriesOnly
			val result: FileChooser.Result.Value = chooser.showOpenDialog(null)
			if (result == FileChooser.Result.Approve) {
				config = updateConfig("paths.user", chooser.selectedFile.getAbsolutePath, "Path to user defined templates")
				saveConfig()
			} else System.exit(0)
		}
		var tmpPaths: Array[Entry[String, ConfigValue]] = Array.empty[Entry[String, ConfigValue]]
		tmpPaths = config.entrySet().toArray(tmpPaths)
		tmpPaths.filter(_.getKey.startsWith("paths.")).foreach(p => paths :+= (p.getKey.replaceAll("paths.", ""), p.getValue.unwrapped().toString))
		GUI.top.visible = true
	}

	def saveConfig() {
		new Write(userDir + "templateCopier.conf").write(config.root().render(ConfigRenderOptions.defaults().setOriginComments(false).setJson(false))).close()
	}

	def updateConfig(path: String, value: Object, comment: String = ""): Config = {
		val list: util.ArrayList[String] = new util.ArrayList[String](1)
		list.add(comment)
		val configValue = ConfigValueFactory.fromAnyRef(value).withOrigin(ConfigOriginFactory.newSimple().withComments(list))
		config = config.withValue(path, configValue)
		config
	}
}
