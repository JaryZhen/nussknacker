package pl.touk.nussknacker.ui.api

import akka.http.scaladsl.server.Directive1
import pl.touk.nussknacker.engine.api.process.ProcessName
import pl.touk.nussknacker.restmodel.process.{ProcessId, ProcessIdWithName, ProcessIdWithNameAndCategory}
import pl.touk.nussknacker.ui.process.repository.FetchingProcessRepository
import pl.touk.nussknacker.ui.process.repository.ProcessRepository.ProcessNotFoundError

import scala.concurrent.{ExecutionContext, Future}

trait ProcessDirectives {
  import akka.http.scaladsl.server.Directives._

  val processRepository: FetchingProcessRepository[Future]
  implicit val ec: ExecutionContext

  def processId(processName: String): Directive1[ProcessIdWithName] = {
    handleExceptions(EspErrorToHttp.espErrorHandler).tflatMap { _ =>
      onSuccess(processRepository.fetchProcessId(ProcessName(processName))).flatMap {
        case Some(processId) => provide(ProcessIdWithName(processId, ProcessName(processName)))
        case None => failWith(ProcessNotFoundError(processName))
      }
    }
  }

  def processIdWithCategory(processName: String): Directive1[ProcessIdWithNameAndCategory] = {
    handleExceptions(EspErrorToHttp.espErrorHandler).tflatMap { _ =>
      onSuccess(processRepository.fetchProcessDetails(ProcessName(processName))).flatMap {
        case Some(details) => provide(ProcessIdWithNameAndCategory(ProcessId(details.id), ProcessName(processName), details.processCategory))
        case None => failWith(ProcessNotFoundError(processName))
      }
    }
  }
}
