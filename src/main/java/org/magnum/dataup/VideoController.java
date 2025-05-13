/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Controller
public class VideoController {

	public static final String DATA_PARAMETER = "data";

	public static final String ID_PARAMETER = "id";

	public static final String VIDEO_SVC_PATH = "/video";

	public static final String VIDEO_DATA_PATH = VIDEO_SVC_PATH + "/{id}/data";

	private static final AtomicLong currentId = new AtomicLong(0L);

	private final Map<Long, Video> videos = new HashMap<>();

	@GetMapping(value = VIDEO_SVC_PATH)
	public @ResponseBody Collection<Video> getVideoList() {

		return videos.values();
	}

	@PostMapping(VIDEO_SVC_PATH)
	public @ResponseBody Video addVideo(@RequestBody Video v) {

		return save(v);
	}

	@GetMapping(value = VIDEO_DATA_PATH)
	public HttpServletResponse getData(@PathVariable(ID_PARAMETER) long id, HttpServletResponse response)
			throws IOException {

		Video v = videos.get(id);
		if (v == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return response;
		}

		response.setContentType("multipart/form-data");
		VideoFileManager videoFileManager = VideoFileManager.get();
		OutputStream out = response.getOutputStream();
		if (videoFileManager.hasVideoData(v)) {
			videoFileManager.copyVideoData(v, out);
			response.setStatus(HttpServletResponse.SC_OK);
		}

		return response;
	}

	@PostMapping(VIDEO_DATA_PATH)
	public @ResponseBody VideoStatus setVideoData(
			@PathVariable(ID_PARAMETER) long id,
			@RequestParam(DATA_PARAMETER) MultipartFile videoData,
			HttpServletResponse response) throws IOException {

		Video v = videos.get(id);
		VideoStatus videoStatus = new VideoStatus(VideoStatus.VideoState.READY);

		if (v == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return videoStatus;
		}

		VideoFileManager videoFileManager = VideoFileManager.get();
		videoFileManager.saveVideoData(v, videoData.getInputStream());
		response.setStatus(HttpServletResponse.SC_OK);
		return videoStatus;
	}

	public Video save(Video entity) {
		checkAndSetId(entity);
		entity.setDataUrl(getDataUrl(entity.getId()));
		videos.put(entity.getId(), entity);
		return entity;
	}

	private void checkAndSetId(Video entity) {
		if(entity.getId() == 0){
			entity.setId(currentId.incrementAndGet());
		}
	}

	private String getDataUrl(long videoId){
        return getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
	}

	private String getUrlBaseForLocalServer() {
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder
				.getRequestAttributes())
				.getRequest();

        return "http://" + request.getServerName()
                + ((request.getServerPort() != 80) ? ":"
                + request.getServerPort() : "");
	}
}
