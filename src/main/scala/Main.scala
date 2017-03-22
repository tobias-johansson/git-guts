import java.io.File

import org.eclipse.jgit.storage.file.FileRepositoryBuilder

object Main extends App {

  import Git._

  val repo =
    new FileRepositoryBuilder().setGitDir(new File("test/.git")).build()

  new Server(repo).serve()
}
