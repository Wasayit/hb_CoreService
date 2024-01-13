package com.bot.coreservice.services;

import com.bot.coreservice.Repository.JobRequirementRepository;
import com.bot.coreservice.Repository.UserPostsRepository;
import com.bot.coreservice.contracts.IUserPostsService;
import com.bot.coreservice.entity.JobRequirement;
import com.bot.coreservice.entity.UserPosts;
import com.bot.coreservice.model.Client;
import com.bot.coreservice.model.DbParameters;
import com.bot.coreservice.model.FileDetail;
import com.bot.coreservice.model.UserPostRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.*;

@Service
public class UserPostsServiceImpl implements IUserPostsService {

    @Autowired
    UserPostsRepository userPostsRepository;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    FileManager fileManager;
    @Autowired
    JobRequirementRepository jobRequirementRepository;

    public String addUserPostService(UserPosts userPost) {
        Date utilDate = new Date();
        var currentDate = new Timestamp(utilDate.getTime());
        var lastUserPostRecord = this.userPostsRepository.getLastUserPostRecord();
        if (lastUserPostRecord == null){
            userPost.setUserPostId(1L);
        }else {
            userPost.setUserPostId(lastUserPostRecord.getUserPostId()+1);
        }
        userPost.setPostedOn(currentDate);
        this.userPostsRepository.save(userPost);

        return "Posted successfully";


    }

    public String updateUserPostService(UserPosts userPost, long userPostId) throws Exception {
        Date utilDate = new Date();
        var currentDate = new Timestamp(utilDate.getTime());
        Optional<UserPosts> result = this.userPostsRepository.findById(userPostId);
        if (result.isEmpty()){
            throw new Exception("No user post found");
        }
        UserPosts existingUserPost = result.get();
        existingUserPost.setShortDescription(userPost.getShortDescription());
        existingUserPost.setCompleteDescription(userPost.getCompleteDescription());
        existingUserPost.setCatagoryTypeId(1);
        existingUserPost.setJobRequirementId(1);
        existingUserPost.setUpdatedOn(currentDate);
        this.userPostsRepository.save(existingUserPost);
        return "User post has been updated";
    }

    public List<UserPosts> getAllUserPosts() {
        var result = userPostsRepository.getAllUserPosts();
        if (result != null && result.size() > 0) {
            result.forEach(x -> {
                if (!Objects.equals(x.getFileDetail(), "[]")){
                    try {
                        x.setFiles(objectMapper.readValue(x.getFileDetail(), new TypeReference<List<FileDetail>>() {
                        }));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        return result;
    }

    public Optional<UserPosts> getUserPostByUserPostIdService(long userPostId) {
        Optional<UserPosts> result = this.userPostsRepository.findById(userPostId);
        return result;
    }

    public String deleteUserPostByUserPostIdService(long userPostId) {
        this.userPostsRepository.deleteById(userPostId);
        return "User post has been deleted";
    }

    @Transactional(rollbackFor = Exception.class)
    public String uploadUserPostsService(String userPost, Flux<FilePart> postImages) throws Exception {
        UserPostRequest userPostRequest = objectMapper.readValue(userPost, UserPostRequest.class);
        UserPosts userPosts = objectMapper.convertValue(userPostRequest, UserPosts.class);
        JobRequirement jobRequirement = objectMapper.convertValue(userPostRequest, JobRequirement.class);

        jobRequirement.setRequiredShortDesc(userPostRequest.getShortDescription());
        jobRequirement.setJobTypeId(userPosts.getCatagoryTypeId());

        var jobRequirementId = addJobRequirement(jobRequirement);
        userPosts.setJobRequirementId(jobRequirementId);
        var filepath = saveUpdateFileDetail(userPostRequest, postImages);
        userPosts.setFileDetail(filepath);
        addUserPostDetailService(userPosts);

        return "New userPost and jobRequirement has been added";
    }

    private void addUserPostDetailService(UserPosts userPosts) {
        Date utilDate = new Date();
        var currentDate = new Timestamp(utilDate.getTime());
        var lastUserPostRecord = this.userPostsRepository.getLastUserPostRecord();
        if (lastUserPostRecord == null)
            userPosts.setUserPostId(1L);
        else
            userPosts.setUserPostId(lastUserPostRecord.getUserPostId()+1);

        userPosts.setPostedBy(1L);
        userPosts.setPostedOn(currentDate);
        this.userPostsRepository.save(userPosts);
    }

    private long addJobRequirement(JobRequirement jobRequirement) {
        Date utilDate = new Date();
        var currentDate = new Timestamp(utilDate.getTime());
        var lastJobRequirementRecord = this.jobRequirementRepository.getLastJobRequirementRecord();
        if (lastJobRequirementRecord == null)
            jobRequirement.setJobRequirementId(1L);
        else
            jobRequirement.setJobRequirementId(lastJobRequirementRecord.getJobRequirementId()+1);

        jobRequirement.setCreatedBy(1L);
        jobRequirement.setCreatedOn(currentDate);
        this.jobRequirementRepository.save(jobRequirement);
        return jobRequirement.getJobRequirementId();
    }

    @Transactional(rollbackFor = Exception.class)
    public String updateUserPostsService(String userPost, Flux<FilePart> postImages) throws Exception {

        UserPostRequest userPostRequest = objectMapper.readValue(userPost, UserPostRequest.class);
        if (userPostRequest.getUserPostId() == 0)
            throw new Exception("Invalid post selected");

        if (userPostRequest.getJobRequirementId() == 0)
            throw new Exception("Invalid Job requirement id");

        updateUserPostService(userPostRequest);
        updateJobRequirementService(userPostRequest);
        saveUpdateFileDetail(userPostRequest, postImages);
        return "userPost and jobRequirement has been updated";
    }

    private void updateUserPostService(UserPostRequest userPostRequest) throws Exception {
        Date utilDate = new Date();
        var currentDate = new Timestamp(utilDate.getTime());
        var data = this.userPostsRepository.findById(userPostRequest.getUserPostId());
        if (data.isEmpty())
            throw new Exception("Post data not found");

        UserPosts existingUserPost = data.get();
        existingUserPost.setShortDescription(userPostRequest.getShortDescription());
        existingUserPost.setCompleteDescription(userPostRequest.getCompleteDescription());
        existingUserPost.setCatagoryTypeId(userPostRequest.getCatagoryTypeId());
        existingUserPost.setUpdatedOn(currentDate);
        this.userPostsRepository.save(existingUserPost);
    }

    private void updateJobRequirementService(UserPostRequest userPostRequest) throws Exception {
        Date utilDate = new Date();
        var currentDate = new Timestamp(utilDate.getTime());
        var result = this.jobRequirementRepository.findById(userPostRequest.getJobRequirementId());
        if (result.isEmpty())
            throw new Exception("JobRequirement record not found");

        JobRequirement existingjobRequirement = result.get();
        existingjobRequirement.setRequiredShortDesc(userPostRequest.getShortDescription());
        existingjobRequirement.setCompleteDescription(userPostRequest.getCompleteDescription());
        existingjobRequirement.setJobTypeId(userPostRequest.getCatagoryTypeId());
        existingjobRequirement.setIsHRAAllowance(userPostRequest.getIsHRAAllowance());
        existingjobRequirement.setHraAllowanceAmount(userPostRequest.getHraAllowanceAmount());
        existingjobRequirement.setIsTravelAllowance(userPostRequest.getIsTravelAllowance());
        existingjobRequirement.setTravelAllowanceAmount(userPostRequest.getTravelAllowanceAmount());
        existingjobRequirement.setIsFoodAllowance(userPostRequest.getIsFoodAllowance());
        existingjobRequirement.setFoodAllowanceAmount(userPostRequest.getFoodAllowanceAmount());
        existingjobRequirement.setIsForeignReturnCompulsory(userPostRequest.getIsForeignReturnCompulsory());
        existingjobRequirement.setMinimunDaysRequired(userPostRequest.getMinimunDaysRequired());
        existingjobRequirement.setMinimunCTC(userPostRequest.getMinimunCTC());
        existingjobRequirement.setMaximunCTC(userPostRequest.getMaximunCTC());
        existingjobRequirement.setIsOTIncluded(userPostRequest.getIsOTIncluded());
        existingjobRequirement.setMaxOTHours(userPostRequest.getMaxOTHours());
        existingjobRequirement.setBonus(userPostRequest.getBonus());
        existingjobRequirement.setCountryId(userPostRequest.getCountryId());
        existingjobRequirement.setMinAgeLimit(userPostRequest.getMinAgeLimit());
        existingjobRequirement.setMaxAgeLimit(userPostRequest.getMaxAgeLimit());
        existingjobRequirement.setNoOfPosts(userPostRequest.getNoOfPosts());
        existingjobRequirement.setSalaryCurrency(userPostRequest.getSalaryCurrency());
        existingjobRequirement.setContractPeriodInMonths(userPostRequest.getContractPeriodInMonths());
        existingjobRequirement.setUpdatedBy(1L);
        existingjobRequirement.setUpdatedOn(currentDate);
    }

    private String saveUpdateFileDetail(UserPostRequest userPostRequest, Flux<FilePart> files) throws Exception {
        String fileDetails = "[]";
        if (files != null) {
            List<FileDetail> existingFiles;
            if (userPostRequest.getFileDetail() != null && userPostRequest.getFileDetail() != "[]") {
                existingFiles = objectMapper.convertValue(userPostRequest.getFileDetail(), new TypeReference<List<FileDetail>>() {
                });
            } else {
                existingFiles = new ArrayList<>();
            }
            files.toIterable().forEach(x -> {
                FileDetail fileDetail = new FileDetail();
                String filepath = null;
                try {
                    filepath = fileManager.uploadFile(x, userPostRequest.getUserPostId(), "post_" + new Date().getTime());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                fileDetail.setFileDetailId(existingFiles.size() + 1);
                fileDetail.setFilePath(filepath);
                existingFiles.add(fileDetail);
            });
            fileDetails = objectMapper.writeValueAsString(existingFiles);
        }
        return fileDetails;
    }

    @Override
    public String deleteImagesService(Long userPostId, int fileDetailId) throws Exception {
        var existingUserPostData = this.userPostsRepository.findById(userPostId);
        var existingUserPost = existingUserPostData.get();
        if (existingUserPost == null){
            throw new Exception("userPostId does not exists");
        }else {
            if (existingUserPost.getFileDetail() != null || existingUserPost.getFileDetail() != "{}"){
                var existingFiles = objectMapper.readValue(existingUserPost.getFileDetail(), new TypeReference<List<FileDetail>>(){
                });
                var updatedFiles = existingFiles.stream().filter(x -> x.getFileDetailId() != fileDetailId).toList();
                existingUserPost.setFileDetail(objectMapper.writeValueAsString(updatedFiles));
                userPostsRepository.save(existingUserPost);
            }
        }
        return "Images has been deleted";
    }
}
