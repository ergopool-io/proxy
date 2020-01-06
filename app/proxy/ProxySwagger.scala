package proxy

import io.swagger.v3.oas.models.media.{Content, MediaType, Schema}
import io.swagger.v3.oas.models.responses.{ApiResponse, ApiResponses}
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.{OpenAPI, Operation, PathItem, Paths}

object ProxySwagger {
  /**
   * Get proxy swagger openAPI
   * @param openAPI the open api to add proxy specifications to it
   * @return
   */
  def getProxyOpenAPI(openAPI: OpenAPI): OpenAPI = {
    addPB(openAPI)

    addShareEndpoint(openAPI)

    openAPI
  }

  /**
   * Add pb to /mining/candidate
   * @param openAPI the object to add pb
   */
  private def addPB(openAPI: OpenAPI): Unit = {
    val pbSchema = new Schema()
    pbSchema.setType("Integer")
    pbSchema.setExample(9876543210L)
    openAPI.getComponents.getSchemas.get("ExternalCandidateBlock").addProperties("pb", pbSchema)
  }

  /**
   * Add share endpoint to swagger
   * @param openAPI the object to add the endpoint
   */
  private def addShareEndpoint(openAPI: OpenAPI): Unit = {
    val response200: ApiResponse = {
      val response = new ApiResponse
      response.setDescription("Share is valid")
      response
    }

    val response500: ApiResponse = {
      val response = new ApiResponse
      response.setDescription("Share is invalid")

      val schema: Schema[_] = new Schema()
      schema.set$ref("#/components/schemas/ApiError")

      val mediaType: MediaType = new MediaType
      mediaType.setSchema(schema)

      val content: Content = new Content
      content.addMediaType("application/json", mediaType)

      response.setContent(content)

      response
    }

    // Put 200 and 500 in responses
    val APIResponses: ApiResponses = {
      val responses = new ApiResponses

      responses.addApiResponse("200", response200)
      responses.addApiResponse("500", response500)

      response500.setDescription("Error")
      responses.setDefault(response500)

      responses
    }

    // Create a post operation
    val postOperation: Operation = {
      val security = new SecurityRequirement
      security.addList("ApiKeyAuth", "[api_key]")

      val op = new Operation
      op.addSecurityItem(security)
      op.setSummary("Submit share for current candidate")
      op.addTagsItem("mining")
      op.setRequestBody(openAPI.getPaths.get("/mining/solution").getPost.getRequestBody)
      op.setResponses(APIResponses)

      op
    }

    // Add post Operation to paths
    val path: PathItem = new PathItem
    path.setPost(postOperation)

    addPath(openAPI, path, "/mining/rewardAddress")
  }

  /**
   * Add pathItem after "after" key
   *
   * @param openAPI the object to add pathItem to
   * @param pathItem the pathItem to add
   * @param after the path that pathItem should be put after it
   */
  private def addPath(openAPI: OpenAPI, pathItem: PathItem, after: String): Unit = {
    // Reformat paths of openAPI
    val newPaths = new Paths
    openAPI.getPaths.forEach({
      case (pathName, pathValue) =>
        newPaths.addPathItem(pathName, pathValue)
        if (pathName == after)
          newPaths.addPathItem("/mining/share", pathItem)
    })
    openAPI.setPaths(newPaths)
  }
}
