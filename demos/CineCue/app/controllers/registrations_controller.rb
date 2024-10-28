class RegistrationsController < ApplicationController

  def signup
    @user = User.new
  end

  def signup_form
    pass_match = params[:user] ? (params[:user][:password] == params[:user][:password_confirmation]) : (params[:password] == params[:password_confirmation])
    if pass_match
      @user = User.new(user_params)
      if @user.save
        session[:user_id] = @user.id
        redirect_to root_path
      else
        render :signup
      end
    else
      flash[:alert] = "Passwords do not match!"
      render :signup
    end
  end

  def login
  end

  def login_form
    user = User.find_by(username: params[:username])
    if user.present? && user.check?(params[:password])
      session[:user_id] = user.id
      redirect_to root_path
    else
      flash[:alert] = "Invalid Username and/or password!"
      render :login
    end
  end

  def edit_pass
    if @user
      render :edit_pass
    else
      redirect_to login_path
    end
  end

  def edit_pass_form
    if @user
      if params[:password] == params[:password_confirmation]
        if @user.update(password_params)
          redirect_to root_path
        else
          flash[:alert] = "Could not save new password!"
          render :edit_pass
        end
      else
        flash[:alert] = "Passwords do not match!"
        render :edit_pass
      end
    else
      redirect_to login_path
    end
  end

  def logout
    session[:user_id] = nil
    redirect_to root_path
  end

  def edit_lang_form
    if @user
      if @user.update(lang_params)
        redirect_to request.referer || root_path
      else
        flash[:alert] = "Could not save new language!"
        render :edit_pass
      end
    else
      cookies[:lang] = lang_params()[:lang]
      redirect_to request.referer || root_path
    end
  end

  private

  def user_params
    params[:user]? (params.require(:user).permit(:username, :email, :password, :lang)) : (params.permit(:username, :email, :password, :lang))
  end

  def password_params
    params.permit(:password)
  end

  def lang_params
    params.permit(:lang)
  end

end
