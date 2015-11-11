package server

import java.rmi.server.UnicastRemoteObject
import java.rmi.registry.Registry
import java.rmi.registry.LocateRegistry
import com.redis._

class Server extends ServerTrait {
  val db: RedisClient = new RedisClient("localhost",6379)

  def createUser(user: String, pass: String): Boolean = {
    val db_user = s"TWEETORRO:USERS:$user:PASSWORD"
    if (db.exists(db_user))
      false
    else
      db.set(db_user,pass)
  }

  def login(user: String, pass: String) : Boolean = {
    val db_user = s"TWEETORRO:USERS:$user:PASSWORD"
    if (db.exists(db_user)) {
      val user_pass = db.get(db_user)
      user_pass match {
        case Some(value) =>
          if (value == pass){
            db.set(s"TWEETORRO:USERS:$user:LOGGED", user)
            true
          }else{
            false
          }
        case None =>
          false
      }
    } else
      false
  }
  def setProfile(nombre: String, alias: String) : Boolean = {
    false
  }
  
  def follow(userLogged:String, userF: String): Boolean = {
    val userToFollow = s"TWEETORRO:USERS:$userF"
    if (db.exists(userToFollow)) {
      db.lpush(s"TWEETORRO:USERS:${db.get(s"TWEETORRO:USERS:$userToFollow:LOGGED")}:Following", userToFollow)
      db.lpush(s"TWEETORRO:USERS:$userToFollow:Followed",db.get(s"TWEETORRO:USERS:$userLogged:LOGGED"))
      println("Añadido following y follower")
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
      val server : ServerTrait = new Server
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
