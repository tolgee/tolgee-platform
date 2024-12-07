<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Travel Bucket List</title>
    <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@400;600&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0-alpha1/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="{{ asset('css/style.css') }}">
</head>
<body>
    <nav class="navbar navbar-expand-lg">
        <div class="container-fluid">
            <a class="navbar-brand heading company-name" href="#">Tolgee</a>
            <button class="btn btn-light ms-auto"  data-bs-toggle="modal" data-bs-target="#addCategoryModal">Add Category</button>
        </div>
        <button class="btn btn-success ms-auto" data-bs-toggle="modal" data-bs-target="#filterByCategoryModal">Filter</button>
    </nav>
    <div class="main-container">
        @if (session('success'))
            <div class="alert alert-success message-tile {{session('class')}}">
                {{ session('success') }}
            </div>
        @endif
        <div id="response"></div>
 
        <div class="container mt-5">
            
            <div class="section">
                <h2 class="heading">Add Bucket List</h2>
                <form id="bucket-list-form" action="{{route('listItem.store')}}" method="POST">
                    @csrf
                    <div class="row">
                        <div class="mb-3 col-md-3">
                            <label for="category" class="form-label">Select Category</label>
                            <select class="form-select" id="category" name="category" required>
                                @if(count($categories))
                                    @foreach ($categories as $categorie)
                                        <option value="{{$categorie->id}}">{{$categorie->name}}</option> 
                                    @endforeach
                                    @else
                                        <option value="Null">--No Category-- </option> 

                                @endif
                            </select>
                        </div>
                        <div class="mb-3 col-md-7">
                            <label for="item" class="form-label">Item</label>
                            <input type="text" class="form-control" id="item" name="item" placeholder="Enter item" required>
                        </div>
                        <div class="col-md-2 d-flex align-items-center mt-3">
                            <button type="submit" class="btn btn-light w-100 gradient-btn">Add</button>
                        </div>
                        {{-- <button type="submit" class="btn btn-light col-md-3">Add</button> --}}
                    </div>
                </form>
            </div>            
        </div>

        <div class="listing-section">
            <div class="section">
                <div class="row align-items-center">
                    <div class="col-md-9">
                        <h2 class="heading">My Todo<span style="text-transform: lowercase;">'ist...</span></h2>
                    </div>
                    <div class="col-md-3 text-end">
                        <input type="date" class="form-control" id="date" name="date" onchange="showTasksDateWise(this)">
                    </div>
                </div>
                
                <div class="list-group" id="bucket-list-items">
                    @if(count($listItems))
                        @foreach ($listItems as $listItem)
                            <div class="list-item list-group-item d-flex justify-content-between align-items-center bg-dark text-white border-0 mb-2 rounded">
                                <div>
                                    <input type="checkbox" {{($listItem->is_completed) ? 'checked' : ''}} id="item1" name="checkItem" onclick="checkItem({{$listItem->id}}, this)">
                                    <label for="item1" class="ms-2 {{($listItem->is_completed) ? 'checked' : ''}}">{{$listItem->title}}</label>
                                    <label for="" class="ms-2">[{{$listItem->categories->name}}]</label>
                                
                                </div>
                                @if($listItem->is_completed)
                                    <span class="badge bg-success rounded-pill task-completion-status" data-id="{{$listItem->id}}">Completed</span>
                                @else
                                    <span class="badge bg-danger rounded-pill task-completion-status" data-id="{{$listItem->id}}">Pending</span>
                                @endif
                                <a href="javascript:void(0)" class="text-decoration-none text-secondary" onclick="editItem({{$listItem->id}})">Edit</a>
                            </div>
                        @endforeach
                    @else
                        No Tasks
                    @endif
                </div>
            </div>
        </div>
    </div>

    {{-- ADD CATEGORY MODAL START--}}
        <div class="modal fade" id="addCategoryModal" tabindex="-1" aria-labelledby="addCategoryModalLabel" aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="addCategoryModalLabel">Add Category</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <form id="addCategoryForm" method="POST" action="{{route('category.store')}}">
                            @csrf
                            <div class="input-group mb-3">
                                <input type="text" class="form-control" id="categoryName" name="categoryName" placeholder="Enter category name" required>
                                <button class="btn btn-outline-secondary" type="submit">Add Category</button>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    </div>
                </div>
            </div>
        </div>
    {{-- ADD CATEGORY MODAL END--}}

    {{-- FILTER CATEGORY MODAL START--}}
        <div class="modal fade" id="filterByCategoryModal" tabindex="-1" aria-labelledby="filterByCategoryModalLabel" aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="addCategoryModalLabel">Add Category</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <h2>Filter</h2>
                        <form id="addCategoryForm" method="POST" onsubmit="return false">
                            @csrf
                            <div class="input-group mb-3">
                                <select class="form-select" id="categoryFilter" name="categoryFilter" required>
                                    @if(count($categories))
                                        @foreach ($categories as $categorie)
                                            <option value="{{$categorie->id}}">{{$categorie->name}}</option> 
                                        @endforeach
                                        @else
                                            <option value="Null">--No Category-- </option> 
                                    @endif
                                </select>
                                <button class="btn btn-outline-success" id="filter" type="submit">Filter</button>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    </div>
                </div>
            </div>
        </div>
    {{-- FILTER CATEGORY MODAL END--}}

    {{-- EDIT ITEM MODAL START--}}
        <div class="modal fade" id="editItemModal" tabindex="-1" aria-labelledby="editItemModalLabel" aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="editItemModalLabel">Edit Item</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <form id="editItemForm" action="{{route('update.listItem')}}">
                            @method('PUT')
                            @csrf
                            <div class="mb-3">
                                <label for="itemTitle" class="form-label text-secondary">Item Title</label>
                                <input type="hidden" name="itemId" id="itemId">
                                <input type="text" class="form-control" placeholder="Item Name here." id="itemTitle" name="itemTitle" required>
                            </div>
                            <button type="submit" class="btn btn-secondary">Edit</button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    {{-- EDIT ITEM MODAL END--}}

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0-alpha1/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>

    <script>
        const updateStatusUrl = "{{ route('update.listItem.status') }}";
        const showItemDateWiseUrl = "{{route('show.listItem.dateWise')}}";
        const showItemCategoryWiseUrl = "{{route('show.listItem.categoryWise')}}";
        const editItemUrl = "{{route('edit.listItem')}}";
        const csrfToken = '{{ csrf_token() }}'; 
    </script>
    
    <script src="{{ asset('js/script.js') }}"></script>

</body>
</html>
