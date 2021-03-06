package pl.touk.nussknacker.ui.api

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, Route}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import pl.touk.nussknacker.engine.ModelData
import pl.touk.nussknacker.engine.ProcessingTypeData.ProcessingType
import pl.touk.nussknacker.ui.NussknackerApp.pathPrefix
import pl.touk.nussknacker.ui.definition
import pl.touk.nussknacker.ui.definition.UIProcessObjects
import pl.touk.nussknacker.ui.process.{ProcessObjectsFinder, ProcessTypesForCategories}
import pl.touk.nussknacker.ui.process.subprocess.SubprocessRepository
import pl.touk.nussknacker.ui.security.api.LoggedUser
import pl.touk.nussknacker.ui.util.EspPathMatchers

import scala.concurrent.ExecutionContext

class DefinitionResources(modelData: Map[ProcessingType, ModelData],
                          subprocessRepository: SubprocessRepository,
                          typesForCategories: ProcessTypesForCategories)
                         (implicit ec: ExecutionContext)
  extends Directives with FailFastCirceSupport with EspPathMatchers with RouteWithUser {

  private val dictResources = new DictResources

  def securedRoute(implicit user: LoggedUser) : Route = encodeResponse {
    path("processDefinitionData" / "componentIds") {
      get {
        complete {
          val subprocessIds = subprocessRepository.loadSubprocesses().map(_.canonical.metaData.id).toList
          ProcessObjectsFinder.componentIds(modelData.values.map(_.processDefinition).toList, subprocessIds)
        }
      }
    } ~ path("processDefinitionData" / "services") {
      get {
        complete {
          modelData.mapValues(_.processDefinition.services.mapValues(definition.UIObjectDefinition(_)))
        }
      }
    // TODO: Now we can't have processingType = componentIds or services - we should redesign our API (probably fetch componentIds and services only for given processingType)
    } ~ pathPrefix("processDefinitionData" / Segment) { processingType =>
      modelData.get(processingType).map { modelDataForType =>
        //TODO maybe always return data for all subprocesses versions instead of fetching just one-by-one?
        pathEndOrSingleSlash {
          post { // POST - because there is sending complex subprocessVersions parameter
            entity(as[Map[String, Long]]) { subprocessVersions =>
              parameter('isSubprocess.as[Boolean]) { (isSubprocess) =>
                val subprocesses = subprocessRepository.loadSubprocesses(subprocessVersions)
                complete(
                  UIProcessObjects.prepareUIProcessObjects(modelDataForType, user, subprocesses, isSubprocess, typesForCategories))
              }
            }
          }
        } ~ dictResources.route(modelDataForType)
      }.getOrElse {
        complete(HttpResponse(status = StatusCodes.NotFound, entity = s"Processing type: $processingType not found"))
      }
    }
  }

}


