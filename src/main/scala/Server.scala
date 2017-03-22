import Git._
import com.twitter.finagle.Http
import com.twitter.finagle.http.Response
import com.twitter.io.Reader
import com.twitter.util.{Await, Future}
import io.finch._
import io.finch.circe._
import io.circe.generic.auto._
import org.eclipse.jgit.lib.Repository

class Server(repo: Repository) {

  object files {
    def file(path: String, contentType: String = "text/html"): Future[Response] = {
      //      Reader.readAll(Reader.fromFile(new File(path))).map { content =>
      val stream = getClass.getResourceAsStream(path)
      Reader.readAll(Reader.fromStream(stream)).map { content =>
        val rep = Response()
        rep.content = content
        rep.contentType = contentType
        rep
      }
    }

    val page: Endpoint[Response] = get("static" :: string) { name: String =>
      file(name)
    }
  }

  object nodes {
    case class Nodes(nodes: Seq[Node])
    case class Node(data: NodeData, classes: String)
    case class NodeData(id: String, label: String)

    val ep: Endpoint[Nodes] = get("api" :: "nodes") {
      val cs = Git.commits(repo).toSeq.map(c => Node(NodeData(c.id, c.message), "commit"))
      val ts = Git.trees(repo).toSeq.map(t => Node(NodeData(t.id, ""), "tree"))
      val fs = Git.files(repo).toSeq.map(f => Node(NodeData(f.id, f.content), "file"))
      Ok(Nodes(cs ++ ts ++ fs))
    }
  }

  object edges {
    case class Edges(edges: Seq[Edge])
    case class Edge(data: EdgeData, classes: String)
    case class EdgeData(source: String, target: String, label: Option[String] = None)

    val ep: Endpoint[Edges] = get("api" :: "edges") {
      val cs = Git.commitEdges(repo).toSeq.map(e => Edge(EdgeData(e.src, e.dst), "commit"))
      val ts = Git.treeEdges(repo).toSeq.map(e => Edge(EdgeData(e.src, e.dst), "tree"))
      val fs = Git.fileEdges(repo).toSeq.map(e => Edge(EdgeData(e.src, e.dst, Some(e.path)), "file"))
      Ok(Edges(cs ++ ts ++ fs))
    }
  }

  def serve() =
    Await.ready(
      Http.server.serve(":9999", (files.page :+: nodes.ep :+: edges.ep).toServiceAs[Application.Json])
    )
}
