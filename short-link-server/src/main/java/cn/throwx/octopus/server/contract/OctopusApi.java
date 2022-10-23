package cn.throwx.octopus.server.contract;

import cn.throwx.octopus.server.contract.request.CreateUrlMapRequest;
import cn.throwx.octopus.server.contract.response.CreateUrlMapResponse;
import cn.throwx.octopus.server.contract.response.Response;

/**
 * @author throwable
 * @version v1
 * @description 短链服务契约
 * @since 2020/12/25 0:12
 */
public interface OctopusApi {

    /**
     * 创建长短链映射
     *
     * @param request request
     * @return Response
     */
    Response<CreateUrlMapResponse> createUrlMap(CreateUrlMapRequest request);
}
