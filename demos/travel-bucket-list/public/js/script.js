function runInterval(){

    var intervalId = setInterval(() => {
        var messageTile = $(".message-tile"); 
        if(messageTile.hasClass('show')){
            messageTile.removeClass('show')
            messageTile.fadeOut();
            clearInterval(intervalId);
        }
    }, 3000);
}
runInterval();
function checkItem(id, checkbox){
    const label = $(checkbox).next('label');
    $.ajax({
        url: updateStatusUrl,
        type: 'POST',
        data: {
            listId: id,
            _token: csrfToken
        },
        success: function(response) {
            ($(checkbox).is(':checked')) ? label.addClass('checked') : label.removeClass('checked'); 
            if (response.status) {
                $('.task-completion-status').each(function() {
                    const element = $(this);
                    if (element.data('id') == response.id) {
                        if (element.hasClass('bg-danger')) {
                            element.removeClass('bg-danger');
                            element.addClass('bg-success');
                            element.text('Completed');
                        }
                    }
                });
            } else {
                $('.task-completion-status').each(function() {
                    const element = $(this);
                    if (element.data('id') == response.id) {
                        if (element.hasClass('bg-success')) {
                            element.removeClass('bg-success');
                            element.addClass('bg-danger');
                            element.text('Pending');
                        }
                    }
                });
            }
            $('#response').html('<div class="alert alert-success message-tile show">' + response.message + '</div>');
            runInterval();
        },
        error: function(xhr) {
            $('#response').html('<div class="alert alert-danger message-tile">An error occurred: ' + xhr.responseText + '</div>');
        }
    });
}

function showTasksDateWise(currentDate){
    $.ajax({
        url: showItemDateWiseUrl,
        type: 'POST',
        data: {
            currentDate: $(currentDate).val(),
            _token: csrfToken
            
        },
        success: function(response) {
            if(response.status == 200){
                $("#bucket-list-items").html('');
                var listItems = response.listItems;
                listItems.forEach(element => {
                    console.log(element);
                    $("#bucket-list-items").append(`
                        <div class="list-item list-group-item d-flex justify-content-between align-items-center bg-dark text-white border-0 mb-2 rounded">
                            <div>
                                <input type="checkbox" ${element.is_completed ? 'checked' : ''} id="item${element.id}" name="checkItem" onclick="checkItem(${element.id}, this)">
                                <label for="item${element.id}" class="ms-2 ${element.is_completed ? 'checked' : ''}">${element.title}</label>
                                <label for="" class="ms-2">[${element.categories.name}]</label>
                            </div>
                            <span class="badge ${element.is_completed ? 'bg-success' : 'bg-danger'} rounded-pill task-completion-status" data-id="${element.id}">
                                ${element.is_completed ? 'Completed' : 'Pending'}
                            </span>
                        </div>
                    `);
                });
            }
            else{
                $("#bucket-list-items").html('No Tasks');
            }
        },
        error: function(xhr) {
            $('#response').html('<div class="alert alert-danger message-tile">An error occurred: ' + xhr.responseText + '</div>');
        }
    });
}

$("#filter").on('click', function(){
    $.ajax({
        url: showItemCategoryWiseUrl,
        type: 'POST',
        data: {
            categoryFilter: $("#categoryFilter").val(),
            _token: csrfToken
        },
        success: function(response) {
            if(response.status == 200){
                $("#bucket-list-items").html('');
                var listItems = response.listItems;
                listItems.forEach(element => {
                    console.log(element.categories.name);
                    $("#bucket-list-items").append(`
                        <div class="list-item list-group-item d-flex justify-content-between align-items-center bg-dark text-white border-0 mb-2 rounded">
                            <div>
                                <input type="checkbox" ${element.is_completed ? 'checked' : ''} id="item${element.id}" name="checkItem" onclick="checkItem(${element.id}, this)">
                                <label for="item${element.id}" class="ms-2 ${element.is_completed ? 'checked' : ''}">${element.title}</label>
                                <label for="" class="ms-2">[${element.categories.name}]</label>
                            </div>
                            <span class="badge ${element.is_completed ? 'bg-success' : 'bg-danger'} rounded-pill task-completion-status" data-id="${element.id}">
                                ${element.is_completed ? 'Completed' : 'Pending'}
                            </span>
                        </div>
                    `);
                });
            }
            else{
                $("#bucket-list-items").html('No Tasks');
            }

            $('#filterByCategoryModal').modal('hide')
        },
        error: function(xhr) {
            $('#response').html('<div class="alert alert-danger message-tile">An error occurred: ' + xhr.responseText + '</div>');
        }
    });
})

function editItem(id){
    $("#editItemModal").modal('show')
    $.ajax({
        url: editItemUrl,
        type: 'POST',
        data: {
            id: id,
            _token: csrfToken
        },
        success: function(response) {
            if(response.status == 200){
                var listItem = response.listItem;
                $("#itemTitle").val(listItem.title)
                $("#itemId").val(listItem.id);
                console.log(listItem.title);
                
            }else{
                $("#bucket-list-items").html('No Tasks');
            }
        },
        error: function(xhr) {
            $('#response').html('<div class="alert alert-danger message-tile">An error occurred: ' + xhr.responseText + '</div>');
        }
    });
}