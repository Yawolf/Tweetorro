package server

import java.rmi.server.UnicastRemoteObject
import java.rmi.registry.Registry
import java.rmi.registry.LocateRegistry
import scala.language.postfixOps
import com.redis._

class Server extends ServerTrait {
  val db: RedisClient = new RedisClient("localhost",6379)

  def createUser(user: String, pass: String): Boolean = {
    val dbUser = s"TWEETORRO:USERS:$user"
    if (db.exists(s"$dbUser:PASSWORD"))
      false
    else {
      db.set(s"$dbUser:PASSWORD",pass)
      db.set(s"$dbUser:ONLINE", false)
    }
  }

  def login(user: String, pass: String) : Boolean = {
    val dbUserPass = s"TWEETORRO:USERS:$user:PASSWORD"
    if (db.exists(dbUserPass)) {
      val user_pass = db.get(dbUserPass)
      user_pass match {
        case Some(value) =>
          value == pass
          db.set(s"TWEETORRO:USERS:$user:LOGGED",true)
        case None =>
          false
      }
    } else
      false
  }

  def setProfile(nombre: String, alias: String) : Boolean = {
    false
  }
  
  def follow(user:String, userF: String): Boolean = {
    val userToFollow = s"TWEETORRO:USERS:$userF"
    if (db.exists(s"$user:PASSWORD")) {
      db.lpush(s"TWEETORRO:USERS:$user:FOLLOWING", userF)
      db.lpush(s"TWEETORRO:USERS:$userF:FOLLOWED", user)
      println("Added following y follower :D")
      true
    } else{
      false
    }
  }

  def unfollow(userLogged: String, userF: String): Boolean = {
    true
  }

}

object Server {
  def main(args: Array[String]): Unit = {
    
    try {
      val server: ServerTrait = new Server
      val stub = UnicastRemoteObject.exportObject(server,0).asInstanceOf[ServerTrait]
      val registry = LocateRegistry.createRegistry(1099)
      registry.rebind("tweetorro", stub)
      
      println("Server ready :D")
    }
    catch {
      case e: Exception => e printStackTrace
    }
    
  }
}
