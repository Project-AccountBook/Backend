import sys

file_path = "/Users/ongsaem/JointLiving/Backend/src/main/java/com/chaewookim/accountbookformoms/domain/grouppruchase/application/GroupPurchaseService.java"

with open(file_path, 'r') as f:
    content = f.read()

# 1. Modify createGroupPurchase
create_target = """        GroupPurchase saved = groupPurchaseRepository.save(groupPurchase);

        String categoryName = groupPurchaseCategoryRepository.findById(request.categoryId())"""

create_replacement = """        GroupPurchase saved = groupPurchaseRepository.save(groupPurchase);
        
        // deduct budget for creator
        deductBudgetForUser(creatorId, request.accountId(), saved);

        String categoryName = groupPurchaseCategoryRepository.findById(request.categoryId())"""

if create_target in content:
    content = content.replace(create_target, create_replacement)
else:
    print("Could not find createGroupPurchase target")

# 2. Modify joinGroupPurchase
join_target = """        GroupPurchaseParticipant participant = GroupPurchaseParticipant.builder()
                .groupPurchaseId(groupPurchaseId)
                .userId(userId)
                .accountId(request.accountId())
                .build();
        groupPurchaseParticipantRepository.save(participant);

        groupPurchase.join();"""

join_replacement = """        GroupPurchaseParticipant participant = GroupPurchaseParticipant.builder()
                .groupPurchaseId(groupPurchaseId)
                .userId(userId)
                .accountId(request.accountId())
                .build();
        groupPurchaseParticipantRepository.save(participant);
        
        // deduct budget for joiner
        deductBudgetForUser(userId, request.accountId(), groupPurchase);

        groupPurchase.join();"""

if join_target in content:
    content = content.replace(join_target, join_replacement)
else:
    print("Could not find joinGroupPurchase target")

# 3. Modify updateGroupPurchaseStatusByAdmin
admin_target = """    @Transactional
    public void updateGroupPurchaseStatusByAdmin(Long id, PurchaseStatus status) {
        GroupPurchase gp = groupPurchaseRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_PURCHASE_NOT_FOUND));
        gp.updateStatusByAdmin(status);
    }"""

admin_replacement = """    @Transactional
    public void updateGroupPurchaseStatusByAdmin(Long id, PurchaseStatus status) {
        GroupPurchase gp = groupPurchaseRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_PURCHASE_NOT_FOUND));
        gp.updateStatusByAdmin(status);
        
        if (status == PurchaseStatus.FAILED) {
            refundBudgetForUser(gp.getCreatorId(), gp.getCreatorAccountId(), gp);
            List<GroupPurchaseParticipant> participants = groupPurchaseParticipantRepository.findByGroupPurchaseId(id);
            for (GroupPurchaseParticipant participant : participants) {
                refundBudgetForUser(participant.getUserId(), participant.getAccountId(), gp);
            }
        }
    }"""

if admin_target in content:
    content = content.replace(admin_target, admin_replacement)
else:
    print("Could not find updateGroupPurchaseStatusByAdmin target")

with open(file_path, 'w') as f:
    f.write(content)

print("Patch applied.")
