require 'faraday'
require 'json'

class MoviesController < ApplicationController\

  @@api_key = "<set tmdb api key here>"

  def index
    if params[:genre]
      genre = params[:genre]
      @movies = fetch_movies_by_genre(genre, 1)
      @movies2 = fetch_movies_by_genre(genre, 2)
    else
      @movies = []
      @movies2 = []
    end
  end

  def book
    if @user
      movie_id = params[:id]
      @user.update!(bookmarks: JSON.generate(JSON.parse(@user.bookmarks || '[]') << movie_id))
      @user.save
    end
    render plain: "Bookmark requested."
  end

  def unbook
    if @user
      movie_id = params[:id]
      @user.update!(bookmarks: JSON.generate(JSON.parse(@user.bookmarks) - [movie_id]))
      @user.save
    end
    redirect_to books_path
  end

  def books
    if @user
      @movies = []
      JSON.parse(@user.bookmarks||"[]").each do |book|
        info = Faraday.get "https://api.themoviedb.org/3/movie/"+book.to_s+"?language="+cookies[:lang]+"&api_key="+@@api_key
        @movies << JSON.parse(info.body)
      end
      render :books
    else
      redirect_to login_path
    end
  end

  private

  def fetch_movies_by_genre(genre, page)

    genreIds = Faraday.get "https://api.themoviedb.org/3/genre/movie/list?language="+cookies[:lang]+"&api_key="+@@api_key

    genres_hash = JSON.parse(genreIds.body)["genres"]

    genre_ids = genre.map do |g|
      genre_pos = genres_hash.find { |gh| gh["name"] == g }
      genre_pos ? genre_pos["id"] : nil
    end.compact.join(",")

    response = Faraday.get "https://api.themoviedb.org/3/discover/movie?include_adult=false&include_video=false&language="+cookies[:lang]+"&page="+page.to_s+"&sort_by=popularity.desc&with_genres="+genre_ids+"&api_key="+@@api_key

    if response.status == 200
      return JSON.parse(response.body)["results"]
    else
      return []
    end

  end

end
